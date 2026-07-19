package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private BigDecimal amount;

    private String razorpayPaymentId;
    private String razorpayOrderId;
    private LocalDateTime paidAt;

    public Payment() {}

    public Payment(Long id, Invoice invoice, BigDecimal amount, String razorpayPaymentId, String razorpayOrderId, LocalDateTime paidAt) {
        this.id = id;
        this.invoice = invoice;
        this.amount = amount;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.paidAt = paidAt;
    }

    @PrePersist
    protected void onCreate() {
        if (paidAt == null) {
            paidAt = LocalDateTime.now();
        }
    }

    public static PaymentBuilder builder() { return new PaymentBuilder(); }

    public static class PaymentBuilder {
        private Long id;
        private Invoice invoice;
        private BigDecimal amount;
        private String razorpayPaymentId;
        private String razorpayOrderId;
        private LocalDateTime paidAt;

        public PaymentBuilder id(Long id) { this.id = id; return this; }
        public PaymentBuilder invoice(Invoice invoice) { this.invoice = invoice; return this; }
        public PaymentBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PaymentBuilder razorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; return this; }
        public PaymentBuilder razorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; return this; }
        public PaymentBuilder paidAt(LocalDateTime paidAt) { this.paidAt = paidAt; return this; }

        public Payment build() {
            return new Payment(id, invoice, amount, razorpayPaymentId, razorpayOrderId, paidAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
