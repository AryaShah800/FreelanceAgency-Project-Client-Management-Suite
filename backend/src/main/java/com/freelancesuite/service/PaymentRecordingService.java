package com.freelancesuite.service;

import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.Payment;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentRecordingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public PaymentRecordingService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    // A separate bean ensures the failed insert rolls back before the caller re-reads
    // the winning payment. The invoice update and successful insert commit together.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment insertPayment(Invoice invoice, String paymentId, String orderId, BigDecimal capturedAmount) {
        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .invoice(invoiceRepository.getReferenceById(invoice.getId()))
                .amount(capturedAmount)
                .razorpayPaymentId(paymentId)
                .razorpayOrderId(orderId)
                .build());
        // Re-read the invoice inside this transaction instead of merging the detached copy.
        Invoice managedInvoice = invoiceRepository.findById(invoice.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        managedInvoice.setStatus(InvoiceStatus.PAID);
        return payment;
    }
}
