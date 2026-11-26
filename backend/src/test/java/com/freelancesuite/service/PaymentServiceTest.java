package com.freelancesuite.service;

import com.freelancesuite.dto.PaymentDto;
import com.freelancesuite.dto.PaymentVerificationRequest;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.Payment;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.PaymentRepository;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.util.HmacSignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private InvoiceRepository invoices;
    private PaymentRepository payments;
    private ExchangeFunction exchange;
    private PaymentService service;
    private Invoice invoice;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        invoices = mock(InvoiceRepository.class);
        payments = mock(PaymentRepository.class);
        exchange = mock(ExchangeFunction.class);
        service = new PaymentService(invoices, payments, new PaymentRecordingService(invoices, payments),
                WebClient.builder().exchangeFunction(exchange), new ObjectMapper(), "key", "key-secret", "webhook-secret");
        Agency agency = Agency.builder().name("Agency").build();
        agency.setId(7L);
        Client client = Client.builder().agency(agency).email("client@example.test").build();
        Project project = Project.builder().client(client).build();
        invoice = Invoice.builder().id(42L).project(project).totalAmount(new BigDecimal("123.45"))
                .status(InvoiceStatus.SENT).build();
        when(invoices.getReferenceById(42L)).thenReturn(invoice);
        principal = new UserPrincipal(3L, "Client", "client@example.test", "", 7L, Role.CLIENT,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    @Test
    void validWebhookRecordsCapturedAmountAndMarksInvoicePaid() {
        when(invoices.findById(42L)).thenReturn(Optional.of(invoice));
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        deliverWebhook(webhook("12345", "INR"));

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(payments).saveAndFlush(saved.capture());
        assertEquals(new BigDecimal("123.45"), saved.getValue().getAmount());
        assertEquals("pay_1", saved.getValue().getRazorpayPaymentId());
        assertEquals("order_1", saved.getValue().getRazorpayOrderId());
        assertSame(invoice, saved.getValue().getInvoice());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        verify(invoices).getReferenceById(42L);
        verify(invoices, times(2)).findById(42L);
        verify(invoices, never()).save(any());
    }

    @Test
    void tamperedWebhookSignatureIsRejectedBeforeReadingInvoice() {
        String original = webhook("12345", "INR");
        assertThrows(AccessDeniedException.class, () -> service.handleWebhookEvent(webhook("1", "INR"),
                HmacSignatureUtil.hmacHex(original, "webhook-secret")));
        verifyNoInteractions(invoices, payments);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "12344", "12346", "12345.5", "null", "\"invalid\""})
    void invalidWebhookAmountIsAcknowledgedWithoutRecording(String amount) {
        when(invoices.findById(42L)).thenReturn(Optional.of(invoice));

        assertDoesNotThrow(() -> deliverWebhook(webhook(amount, "INR")));

        verifyNoInteractions(payments);
        verify(invoices, never()).save(any());
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());
    }

    @Test
    void webhookCurrencyMismatchIsAcknowledgedWithoutRecording() {
        when(invoices.findById(42L)).thenReturn(Optional.of(invoice));
        assertDoesNotThrow(() -> deliverWebhook(webhook("12345", "USD")));
        verifyNoInteractions(payments);
        verify(invoices, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceStatus.class, names = {"SENT", "OVERDUE"}, mode = EnumSource.Mode.EXCLUDE)
    void nonPayableWebhookIsAcknowledgedWithoutRecording(InvoiceStatus status) {
        invoice.setStatus(status);
        when(invoices.findById(42L)).thenReturn(Optional.of(invoice));
        assertDoesNotThrow(() -> deliverWebhook(webhook("12345", "INR")));
        verifyNoInteractions(payments);
        verify(invoices, never()).save(any());
    }

    @Test
    void duplicateWebhookDoesNotInsertAgain() {
        when(invoices.findById(42L)).thenReturn(Optional.of(invoice));
        when(payments.findByRazorpayPaymentId("pay_1")).thenReturn(Optional.of(existingPayment()));

        assertDoesNotThrow(() -> deliverWebhook(webhook("12345", "INR")));

        verify(payments, never()).saveAndFlush(any());
        verify(invoices, never()).save(any());
    }

    @Test
    void repeatedWebhookAfterSuccessDoesNotInsertAgain() {
        when(invoices.findById(42L)).thenReturn(Optional.of(invoice));
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        deliverWebhook(webhook("12345", "INR"));
        assertDoesNotThrow(() -> deliverWebhook(webhook("12345", "INR")));

        verify(payments, times(1)).saveAndFlush(any());
    }

    @Test
    void webhookInsertRaceIsAcknowledged() {
        when(invoices.findById(42L)).thenReturn(Optional.of(invoice));
        when(payments.findByRazorpayPaymentId("pay_1"))
                .thenReturn(Optional.empty(), Optional.of(existingPayment()));
        when(payments.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertDoesNotThrow(() -> deliverWebhook(webhook("12345", "INR")));

        verify(payments, times(2)).findByRazorpayPaymentId("pay_1");
        verify(invoices, never()).save(any());
    }

    @Test
    void verificationInsertRaceReturnsWinningPayment() {
        stubVerification("12345", "INR", "12345", "INR", "captured", "order_1");
        Payment winner = existingPayment();
        when(payments.findByRazorpayPaymentId("pay_1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(payments.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertPaymentDto(winner, service.verifyAndRecordPayment(request(), principal));

        verify(payments, times(2)).findByRazorpayPaymentId("pay_1");
        verify(invoices, never()).save(any());
    }

    @Test
    void unrelatedIntegrityFailureIsNotSwallowed() {
        stubVerification("12345", "INR", "12345", "INR", "captured", "order_1");
        DataIntegrityViolationException error = new DataIntegrityViolationException("other constraint");
        when(payments.saveAndFlush(any())).thenThrow(error);
        assertSame(error, assertThrows(DataIntegrityViolationException.class,
                () -> service.verifyAndRecordPayment(request(), principal)));
    }

    @Test
    void validVerificationRecordsCapturedAmount() {
        stubVerification("12345", "INR", "12345", "INR", "captured", "order_1");
        Invoice managedInvoice = Invoice.builder().id(42L).status(InvoiceStatus.SENT).build();
        when(invoices.findById(42L)).thenReturn(Optional.of(managedInvoice));
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        PaymentDto payment = service.verifyAndRecordPayment(request(), principal);

        assertEquals(new BigDecimal("123.45"), payment.amount());
        assertEquals(42L, payment.invoiceId());
        assertEquals("pay_1", payment.razorpayPaymentId());
        assertEquals("order_1", payment.razorpayOrderId());
        assertEquals(InvoiceStatus.PAID, managedInvoice.getStatus());
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());
        verify(invoices).getReferenceById(42L);
        verify(invoices).findById(42L);
        verify(invoices, never()).save(any());
    }

    @Test
    void verificationDuplicateReturnsExistingPaymentEvenIfInvoiceIsPaid() {
        stubVerification("12345", "INR", "12345", "INR", "captured", "order_1");
        invoice.setStatus(InvoiceStatus.PAID);
        Payment existing = existingPayment();
        when(payments.findByRazorpayPaymentId("pay_1")).thenReturn(Optional.of(existing));
        assertPaymentDto(existing, service.verifyAndRecordPayment(request(), principal));
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void verificationOrderAmountMismatchIsRejected() {
        stubVerification("1", "INR", "12345", "INR", "captured", "order_1");
        assertVerificationRejected();
    }

    @Test
    void verificationCapturedAmountMismatchIsRejected() {
        stubVerification("12345", "INR", "1", "INR", "captured", "order_1");
        assertVerificationRejected();
    }

    @Test
    void verificationOrderCurrencyMismatchIsRejected() {
        stubVerification("12345", "USD", "12345", "INR", "captured", "order_1");
        assertVerificationRejected();
    }

    @Test
    void verificationCapturedCurrencyMismatchIsRejected() {
        stubVerification("12345", "INR", "12345", "USD", "captured", "order_1");
        assertVerificationRejected();
    }

    @Test
    void verificationUncapturedPaymentIsRejected() {
        stubVerification("12345", "INR", "12345", "INR", "authorized", "order_1");
        assertVerificationRejected();
    }

    @Test
    void verificationPaymentFromAnotherOrderIsRejected() {
        stubVerification("12345", "INR", "12345", "INR", "captured", "order_other");
        assertVerificationRejected();
    }

    @Test
    void verificationNonPayableInvoiceIsRejectedWithConflict() {
        stubVerification("12345", "INR", "12345", "INR", "captured", "order_1");
        invoice.setStatus(InvoiceStatus.DRAFT);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.verifyAndRecordPayment(request(), principal));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(payments, never()).saveAndFlush(any());
    }

    private void assertVerificationRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.verifyAndRecordPayment(request(), principal));
        verifyNoInteractions(payments);
        verify(invoices, never()).save(any());
    }

    private void stubVerification(String orderAmount, String orderCurrency, String paymentAmount,
                                  String paymentCurrency, String status, String orderId) {
        when(invoices.findByIdWithProjectOwnership(42L)).thenReturn(Optional.of(invoice));
        String order = """
                {"notes":{"invoiceId":"42"},"amount":%s,"currency":"%s"}
                """.formatted(orderAmount, orderCurrency);
        String payment = """
                {"id":"pay_1","order_id":"%s","status":"%s","amount":%s,"currency":"%s"}
                """.formatted(orderId, status, paymentAmount, paymentCurrency);
        when(exchange.exchange(any())).thenAnswer(call -> {
            String path = ((org.springframework.web.reactive.function.client.ClientRequest) call.getArgument(0)).url().getPath();
            String body = switch (path) {
                case "/v1/orders/order_1" -> order;
                case "/v1/payments/pay_1" -> payment;
                default -> throw new AssertionError("Unexpected Razorpay request: " + path);
            };
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                    .body(body).build());
        });
    }

    private void assertPaymentDto(Payment expected, PaymentDto actual) {
        assertEquals(expected.getId(), actual.id());
        assertEquals(expected.getInvoice().getId(), actual.invoiceId());
        assertEquals(expected.getAmount(), actual.amount());
        assertEquals(expected.getRazorpayPaymentId(), actual.razorpayPaymentId());
        assertEquals(expected.getRazorpayOrderId(), actual.razorpayOrderId());
        assertEquals(expected.getPaidAt(), actual.paidAt());
    }

    private Payment existingPayment() {
        return Payment.builder().id(9L).invoice(invoice).amount(new BigDecimal("123.45"))
                .razorpayPaymentId("pay_1").razorpayOrderId("order_1").build();
    }

    private PaymentVerificationRequest request() {
        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setInvoiceId(42L);
        request.setRazorpayOrderId("order_1");
        request.setRazorpayPaymentId("pay_1");
        request.setRazorpaySignature(HmacSignatureUtil.hmacHex("order_1|pay_1", "key-secret"));
        return request;
    }

    private String webhook(String amount, String currency) {
        return """
                {"event":"payment.captured","payload":{"payment":{"entity":{
                  "id":"pay_1","order_id":"order_1","notes":{"invoiceId":"42"},
                  "amount":%s,"currency":"%s"
                }}}}
                """.formatted(amount, currency);
    }

    private void deliverWebhook(String payload) {
        service.handleWebhookEvent(payload, HmacSignatureUtil.hmacHex(payload, "webhook-secret"));
    }
}
