package com.freelancesuite.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.PaymentRepository;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentConfigurationTest {

    @Test
    void disabledCheckoutReturnsServiceUnavailableBeforeLookingUpInvoice() {
        PaymentService service = new PaymentService(
                null, null, null, new ObjectMapper(), "", "", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createRazorpayOrder(42L, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void checkoutStaysDisabledUntilWebhookSecretIsConfigured() {
        PaymentService service = new PaymentService(null, null, null, new ObjectMapper(),
                "rzp_test_key", "test-key-secret", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createRazorpayOrder(42L, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void draftInvoiceCannotStartPaymentEvenWhenProviderIsConfigured() {
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentService service = new PaymentService(invoices, payments, null,
                new ObjectMapper(), "rzp_test_key", "test-key-secret", "webhook-secret");
        Agency agency = Agency.builder().name("Test agency").build();
        agency.setId(7L);
        Client client = Client.builder().agency(agency).companyName("Test client")
                .email("client@example.test").build();
        Project project = Project.builder().client(client).title("Test project").build();
        Invoice draft = Invoice.builder().id(42L).project(project).invoiceNumber("INV-TEST")
                .subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).dueDate(LocalDate.now())
                .status(InvoiceStatus.DRAFT).build();
        when(invoices.findByIdWithProjectOwnership(42L)).thenReturn(Optional.of(draft));
        UserPrincipal principal = new UserPrincipal(3L, "Client", "client@example.test", "",
                7L, Role.CLIENT, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createRazorpayOrder(42L, principal));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(payments);
    }
}
