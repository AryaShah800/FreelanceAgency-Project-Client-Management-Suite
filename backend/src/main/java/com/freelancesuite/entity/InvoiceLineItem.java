package com.freelancesuite.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal amount;

    public InvoiceLineItem() {}

    public InvoiceLineItem(Long id, Invoice invoice, String description, Integer quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.id = id;
        this.invoice = invoice;
        this.description = description;
        this.quantity = quantity != null ? quantity : 1;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    public static InvoiceLineItemBuilder builder() { return new InvoiceLineItemBuilder(); }

    public static class InvoiceLineItemBuilder {
        private Long id;
        private Invoice invoice;
        private String description;
        private Integer quantity = 1;
        private BigDecimal unitPrice;
        private BigDecimal amount;

        public InvoiceLineItemBuilder id(Long id) { this.id = id; return this; }
        public InvoiceLineItemBuilder invoice(Invoice invoice) { this.invoice = invoice; return this; }
        public InvoiceLineItemBuilder description(String description) { this.description = description; return this; }
        public InvoiceLineItemBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public InvoiceLineItemBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public InvoiceLineItemBuilder amount(BigDecimal amount) { this.amount = amount; return this; }

        public InvoiceLineItem build() {
            return new InvoiceLineItem(id, invoice, description, quantity, unitPrice, amount);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
