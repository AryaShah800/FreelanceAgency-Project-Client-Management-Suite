package com.freelancesuite.service;

import com.freelancesuite.controller.PaymentController;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Test
    void verifyPaymentReturnsDtoWithInvoiceIdAndSavedPaymentFields() {
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        ExchangeFunction exchange = mock(ExchangeFunction.class);
        when(exchange.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"notes\":{\"invoiceId\":\"42\"}}")
                .build()));
        PaymentService service = new PaymentService(invoices, payments,
                WebClient.builder().exchangeFunction(exchange), new ObjectMapper(),
                "rzp_test_key", "test-key-secret", "webhook-secret");
        PaymentController controller = new PaymentController(service);

        Agency agency = Agency.builder().id(7L).build();
        Client client = Client.builder().agency(agency).email("client@example.test").build();
        Project project = Project.builder().client(client).build();
        Invoice invoice = Invoice.builder().id(42L).project(project)
                .totalAmount(new BigDecimal("1500.00")).status(InvoiceStatus.SENT).build();
        when(invoices.findByIdWithProjectOwnership(42L)).thenReturn(Optional.of(invoice));
        when(payments.findByRazorpayPaymentId("pay_test")).thenReturn(Optional.empty());
        LocalDateTime paidAt = LocalDateTime.of(2026, 1, 2, 10, 30);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(11L);
            payment.setPaidAt(paidAt);
            return payment;
        });
        UserPrincipal principal = new UserPrincipal(3L, "Client", "client@example.test", "",
                7L, Role.CLIENT, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setInvoiceId(42L);
        request.setRazorpayOrderId("order_test");
        request.setRazorpayPaymentId("pay_test");
        request.setRazorpaySignature(HmacSignatureUtil.hmacHex("order_test|pay_test", "test-key-secret"));

        ResponseEntity<PaymentDto> response = controller.verifyPayment(request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new PaymentDto(11L, 42L, new BigDecimal("1500.00"),
                "pay_test", "order_test", paidAt), response.getBody());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        verify(invoices).save(invoice);
        verify(payments).save(any(Payment.class));
    }
}
