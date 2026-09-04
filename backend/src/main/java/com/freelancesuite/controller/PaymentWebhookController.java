package com.freelancesuite.controller;

import com.freelancesuite.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deliberately under /api/v1/public/** (permitAll in SecurityConfig): Razorpay
 * calls this server-to-server with no user session, authenticated only by the
 * HMAC signature in X-Razorpay-Signature — verified against RAZORPAY_WEBHOOK_SECRET
 * inside PaymentService, not by Spring Security.
 */
@RestController
@RequestMapping("/api/v1/public/payments")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhookEvent(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
