package com.freelancesuite.controller;

import com.freelancesuite.dto.PaymentVerificationRequest;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.entity.Payment;
import com.freelancesuite.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('CLIENT')")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestParam Long invoiceId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(invoiceId, userPrincipal));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<Payment> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentService.verifyAndRecordPayment(request, userPrincipal));
    }
}
