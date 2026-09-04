package com.freelancesuite.service;

import com.freelancesuite.dto.PaymentVerificationRequest;
import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.Payment;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.PaymentRepository;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

@Service
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final WebClient webClient;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;

    @Autowired
    public PaymentService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository,
                          WebClient.Builder webClientBuilder,
                          @Value("${app.razorpay.key-id:}") String razorpayKeyId,
                          @Value("${app.razorpay.key-secret:}") String razorpayKeySecret) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.razorpay.com/v1").build();
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    public Map<String, Object> createRazorpayOrder(Long invoiceId, UserPrincipal userPrincipal) {
        Invoice invoice = requireClientInvoice(invoiceId, userPrincipal);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("This invoice has already been paid");
        }
        ensureRazorpayConfigured();
        long amountInPaise = invoice.getTotalAmount().multiply(new BigDecimal("100")).longValue();

        Map<String, Object> providerOrder = webClient.post()
                .uri("/orders")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .headers(headers -> headers.setBasicAuth(razorpayKeyId, razorpayKeySecret, StandardCharsets.UTF_8))
                .bodyValue(Map.of("amount", amountInPaise, "currency", "INR", "receipt", "invoice_" + invoice.getId()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (providerOrder == null || providerOrder.get("id") == null) {
            throw new IllegalArgumentException("Payment provider did not return an order");
        }
        return Map.of(
            "keyId", razorpayKeyId,
            "orderId", providerOrder.get("id"),
            "amount", amountInPaise,
            "currency", "INR",
            "invoiceId", invoice.getId(),
            "invoiceNumber", invoice.getInvoiceNumber(),
            "clientName", invoice.getProject().getClient().getCompanyName()
        );
    }

    @Transactional
    public Payment verifyAndRecordPayment(PaymentVerificationRequest request, UserPrincipal userPrincipal) {
        Invoice invoice = requireClientInvoice(request.getInvoiceId(), userPrincipal);
        ensureRazorpayConfigured();
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("This invoice has already been paid");
        }
        if (!isValidSignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature())) {
            throw new IllegalArgumentException("Payment signature verification failed");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(invoice.getTotalAmount())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .razorpayOrderId(request.getRazorpayOrderId())
                .build();

        return paymentRepository.save(payment);
    }

    private Invoice requireClientInvoice(Long invoiceId, UserPrincipal userPrincipal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (!invoice.getProject().getClient().getAgency().getId().equals(userPrincipal.getAgencyId())
                || !invoice.getProject().getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot pay this invoice");
        }
        return invoice;
    }

    private void ensureRazorpayConfigured() {
        if (razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()) {
            throw new IllegalArgumentException("Online payments are not configured for this workspace");
        }
    }

    private boolean isValidSignature(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            byte[] provided = hexToBytes(signature);
            return MessageDigest.isEqual(expected, provided);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not verify the payment signature", ex);
        }
    }

    private byte[] hexToBytes(String value) {
        if (value.length() % 2 != 0 || !value.matches("[0-9a-fA-F]+")) {
            return new byte[0];
        }
        byte[] bytes = new byte[value.length() / 2];
        for (int index = 0; index < value.length(); index += 2) {
            bytes[index / 2] = (byte) Integer.parseInt(value.substring(index, index + 2), 16);
        }
        return bytes;
    }
}
