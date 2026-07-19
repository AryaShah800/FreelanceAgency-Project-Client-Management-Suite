package com.freelancesuite.controller;

import com.freelancesuite.entity.Payment;
import com.freelancesuite.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestParam Long invoiceId) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(invoiceId));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<Payment> verifyPayment(
            @RequestParam Long invoiceId,
            @RequestParam String paymentId,
            @RequestParam String orderId) {
        return ResponseEntity.ok(paymentService.verifyAndRecordPayment(invoiceId, paymentId, orderId));
    }
}
