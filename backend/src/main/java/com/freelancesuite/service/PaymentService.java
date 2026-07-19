package com.freelancesuite.service;

import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.Payment;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public Map<String, Object> createRazorpayOrder(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        long amountInPaise = invoice.getTotalAmount().multiply(new BigDecimal("100")).longValue();

        return Map.of(
            "orderId", orderId,
            "amount", amountInPaise,
            "currency", "INR",
            "invoiceId", invoice.getId(),
            "invoiceNumber", invoice.getInvoiceNumber(),
            "clientName", invoice.getProject().getClient().getCompanyName()
        );
    }

    @Transactional
    public Payment verifyAndRecordPayment(Long invoiceId, String razorpayPaymentId, String razorpayOrderId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(invoice.getTotalAmount())
                .razorpayPaymentId(razorpayPaymentId)
                .razorpayOrderId(razorpayOrderId)
                .build();

        return paymentRepository.save(payment);
    }
}
