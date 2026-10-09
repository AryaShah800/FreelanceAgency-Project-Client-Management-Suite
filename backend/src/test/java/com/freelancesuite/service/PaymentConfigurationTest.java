package com.freelancesuite.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
}
