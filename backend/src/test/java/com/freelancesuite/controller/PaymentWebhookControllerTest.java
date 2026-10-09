package com.freelancesuite.controller;

import com.freelancesuite.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this controller test
@ActiveProfiles("test")
public class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private final String payload = "{\"event\":\"payment.captured\"}";
    private final String validSignature = "valid_signature";

    @BeforeEach
    void setUp() {
        // Assume service validates successfully by default
    }

    @Test
    void testValidWebhookSignature() throws Exception {
        mockMvc.perform(post("/api/v1/public/payments/webhook")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", validSignature))
                .andExpect(status().isOk());

        verify(paymentService).handleWebhookEvent(eq(payload), eq(validSignature));
    }

    @Test
    void testInvalidWebhookSignature() throws Exception {
        doThrow(new SecurityException("Invalid signature")).when(paymentService).handleWebhookEvent(anyString(), anyString());

        mockMvc.perform(post("/api/v1/public/payments/webhook")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", "invalid_signature"))
                .andExpect(status().isForbidden()); // Assuming error handling maps SecurityException to 403, or throws exception mapped to 500. Let's see what it does.
    }

    @Test
    void testMissingWebhookSignature() throws Exception {
        doThrow(new SecurityException("Missing signature")).when(paymentService).handleWebhookEvent(anyString(), isNull());

        mockMvc.perform(post("/api/v1/public/payments/webhook")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
