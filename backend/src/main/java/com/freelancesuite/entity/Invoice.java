package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.InvoiceStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal igst = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false)
    private LocalDate dueDate;

    private Boolean isRecurring = false;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    private LocalDateTime createdAt;

    public Invoice() {}

    public Invoice(Long id, Project project, String invoiceNumber, BigDecimal subtotal, BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal totalAmount, InvoiceStatus status, LocalDate dueDate, Boolean isRecurring, List<InvoiceLineItem> lineItems, LocalDateTime createdAt) {
        this.id = id;
        this.project = project;
        this.invoiceNumber = invoiceNumber;
        this.subtotal = subtotal;
        this.cgst = cgst != null ? cgst : BigDecimal.ZERO;
        this.sgst = sgst != null ? sgst : BigDecimal.ZERO;
        this.igst = igst != null ? igst : BigDecimal.ZERO;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : InvoiceStatus.DRAFT;
        this.dueDate = dueDate;
        this.isRecurring = isRecurring != null ? isRecurring : false;
        this.lineItems = lineItems != null ? lineItems : new ArrayList<>();
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static InvoiceBuilder builder() { return new InvoiceBuilder(); }

    public static class InvoiceBuilder {
        private Long id;
        private Project project;
        private String invoiceNumber;
        private BigDecimal subtotal;
        private BigDecimal cgst = BigDecimal.ZERO;
        private BigDecimal sgst = BigDecimal.ZERO;
        private BigDecimal igst = BigDecimal.ZERO;
        private BigDecimal totalAmount;
        private InvoiceStatus status = InvoiceStatus.DRAFT;
        private LocalDate dueDate;
        private Boolean isRecurring = false;
        private List<InvoiceLineItem> lineItems = new ArrayList<>();
        private LocalDateTime createdAt;

        public InvoiceBuilder id(Long id) { this.id = id; return this; }
        public InvoiceBuilder project(Project project) { this.project = project; return this; }
        public InvoiceBuilder invoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; return this; }
        public InvoiceBuilder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public InvoiceBuilder cgst(BigDecimal cgst) { this.cgst = cgst; return this; }
        public InvoiceBuilder sgst(BigDecimal sgst) { this.sgst = sgst; return this; }
        public InvoiceBuilder igst(BigDecimal igst) { this.igst = igst; return this; }
        public InvoiceBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public InvoiceBuilder status(InvoiceStatus status) { this.status = status; return this; }
        public InvoiceBuilder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public InvoiceBuilder isRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; return this; }
        public InvoiceBuilder lineItems(List<InvoiceLineItem> lineItems) { this.lineItems = lineItems; return this; }
        public InvoiceBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Invoice build() {
            return new Invoice(id, project, invoiceNumber, subtotal, cgst, sgst, igst, totalAmount, status, dueDate, isRecurring, lineItems, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getCgst() { return cgst; }
    public void setCgst(BigDecimal cgst) { this.cgst = cgst; }
    public BigDecimal getSgst() { return sgst; }
    public void setSgst(BigDecimal sgst) { this.sgst = sgst; }
    public BigDecimal getIgst() { return igst; }
    public void setIgst(BigDecimal igst) { this.igst = igst; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; }
    public List<InvoiceLineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<InvoiceLineItem> lineItems) { this.lineItems = lineItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
