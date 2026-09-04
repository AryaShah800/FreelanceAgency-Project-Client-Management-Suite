package com.freelancesuite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelancesuite.dto.PaymentVerificationRequest;
import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.Payment;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.PaymentRepository;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.util.HmacSignatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;

    @Autowired
    public PaymentService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository,
                          WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                          @Value("${app.razorpay.key-id:}") String razorpayKeyId,
                          @Value("${app.razorpay.key-secret:}") String razorpayKeySecret,
                          @Value("${app.razorpay.webhook-secret:}") String razorpayWebhookSecret) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.razorpay.com/v1").build();
        this.objectMapper = objectMapper;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
    }

    public Map<String, Object> createRazorpayOrder(Long invoiceId, UserPrincipal userPrincipal) {
        Invoice invoice = requireClientInvoice(invoiceId, userPrincipal);
        ensureConfigured();

        BigDecimal total = invoice.getTotalAmount();
        long amountInPaise = total.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map response = webClient.post()
                .uri("/orders")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .headers(headers -> headers.setBasicAuth(razorpayKeyId, razorpayKeySecret, StandardCharsets.UTF_8))
                .bodyValue(Map.of(
                        "amount", amountInPaise,
                        "currency", "INR",
                        "receipt", "invoice_" + invoice.getId(),
                        // Carried through to the webhook payload so the webhook handler can
                        // reconcile a payment.captured event back to an invoice without
                        // trusting anything the client sends.
                        "notes", Map.of("invoiceId", String.valueOf(invoice.getId()))
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("id")) {
            throw new IllegalStateException("Could not create Razorpay order");
        }

        return Map.of(
                "orderId", response.get("id"),
                "amount", amountInPaise,
                "currency", "INR",
                "keyId", razorpayKeyId,
                "invoiceId", invoice.getId()
        );
    }

    /**
     * Client-triggered confirmation, called right after Razorpay Checkout succeeds
     * in the browser. Gives the user instant feedback. Because the HMAC check can't
     * be forged without the key secret, this alone can't be exploited to mark an
     * unpaid invoice as paid — but if the tab closes before this call fires, the
     * invoice would never be reconciled. handleWebhookEvent() below is the
     * server-to-server source of truth that covers that gap.
     */
    @Transactional
    public Payment verifyAndRecordPayment(PaymentVerificationRequest request, UserPrincipal userPrincipal) {
        Invoice invoice = requireClientInvoice(request.getInvoiceId(), userPrincipal);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("This invoice has already been paid");
        }
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        if (!HmacSignatureUtil.verify(payload, razorpayKeySecret, request.getRazorpaySignature())) {
            throw new IllegalArgumentException("Payment signature verification failed");
        }

        return recordPaymentIfAbsent(invoice, request.getRazorpayPaymentId(), request.getRazorpayOrderId());
    }

    /**
     * Server-to-server Razorpay webhook handler (payment.captured). This is the
     * authoritative confirmation path: independent of whether the client's browser
     * stayed open, Razorpay calls this directly once the charge is captured.
     *
     * @param rawBody        the exact raw request body, unmodified (needed for signature verification)
     * @param signatureHeader the X-Razorpay-Signature header value
     */
    @Transactional
    public void handleWebhookEvent(String rawBody, String signatureHeader) {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            log.warn("Received a Razorpay webhook call but RAZORPAY_WEBHOOK_SECRET is not configured — ignoring.");
            throw new IllegalStateException("Webhook is not configured for this workspace");
        }
        if (!HmacSignatureUtil.verify(rawBody, razorpayWebhookSecret, signatureHeader)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Malformed webhook payload", ex);
        }

        String event = root.path("event").asText("");
        if (!"payment.captured".equals(event)) {
            // We only act on captured payments; other event types (order.paid,
            // payment.failed, refunds, etc.) are safely ignored here.
            return;
        }

        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String razorpayPaymentId = paymentEntity.path("id").asText(null);
        String razorpayOrderId = paymentEntity.path("order_id").asText(null);
        String invoiceIdStr = paymentEntity.path("notes").path("invoiceId").asText(null);

        if (razorpayPaymentId == null || razorpayOrderId == null || invoiceIdStr == null) {
            log.warn("payment.captured webhook missing expected fields; paymentId={}, orderId={}, invoiceId note={}",
                    razorpayPaymentId, razorpayOrderId, invoiceIdStr);
            throw new IllegalArgumentException("Webhook payload is missing required fields");
        }

        Long invoiceId;
        try {
            invoiceId = Long.valueOf(invoiceIdStr);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Webhook payload had a non-numeric invoiceId note");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook references an unknown invoice: " + invoiceId));

        recordPaymentIfAbsent(invoice, razorpayPaymentId, razorpayOrderId);
    }

    /**
     * Idempotent: a captured payment may be confirmed by both the client-triggered
     * verify() call and the webhook, or the webhook may be retried by Razorpay.
     * Safe to call more than once for the same razorpayPaymentId.
     */
    private Payment recordPaymentIfAbsent(Invoice invoice, String razorpayPaymentId, String razorpayOrderId) {
        Optional<Payment> existing = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(invoice.getTotalAmount())
                .razorpayPaymentId(razorpayPaymentId)
                .razorpayOrderId(razorpayOrderId)
                .build();

        return paymentRepository.save(payment);
    }

    private Invoice requireClientInvoice(Long invoiceId, UserPrincipal userPrincipal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (!invoice.getProject().getClient().getAgency().getId().equals(userPrincipal.getAgencyId())) {
            throw new IllegalArgumentException("Unauthorized invoice access");
        }
        return invoice;
    }

    private void ensureConfigured() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new IllegalArgumentException("Online payments are not configured for this workspace");
        }
    }
}
