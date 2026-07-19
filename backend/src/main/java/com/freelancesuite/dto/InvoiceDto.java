package com.freelancesuite.dto;

import com.freelancesuite.entity.enums.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class InvoiceDto {
    private Long id;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private String projectTitle;
    private String clientName;
    private String invoiceNumber;

    private BigDecimal subtotal;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal totalAmount;

    private InvoiceStatus status;
    private LocalDate dueDate;
    private Boolean isRecurring;

    private List<InvoiceLineItemDto> lineItems;
    private LocalDateTime createdAt;

    public InvoiceDto() {}

    public InvoiceDto(Long id, Long projectId, String projectTitle, String clientName, String invoiceNumber, BigDecimal subtotal, BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal totalAmount, InvoiceStatus status, LocalDate dueDate, Boolean isRecurring, List<InvoiceLineItemDto> lineItems, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.clientName = clientName;
        this.invoiceNumber = invoiceNumber;
        this.subtotal = subtotal;
        this.cgst = cgst;
        this.sgst = sgst;
        this.igst = igst;
        this.totalAmount = totalAmount;
        this.status = status;
        this.dueDate = dueDate;
        this.isRecurring = isRecurring;
        this.lineItems = lineItems;
        this.createdAt = createdAt;
    }

    public static InvoiceDtoBuilder builder() { return new InvoiceDtoBuilder(); }

    public static class InvoiceDtoBuilder {
        private Long id;
        private Long projectId;
        private String projectTitle;
        private String clientName;
        private String invoiceNumber;
        private BigDecimal subtotal;
        private BigDecimal cgst;
        private BigDecimal sgst;
        private BigDecimal igst;
        private BigDecimal totalAmount;
        private InvoiceStatus status;
        private LocalDate dueDate;
        private Boolean isRecurring;
        private List<InvoiceLineItemDto> lineItems;
        private LocalDateTime createdAt;

        public InvoiceDtoBuilder id(Long id) { this.id = id; return this; }
        public InvoiceDtoBuilder projectId(Long projectId) { this.projectId = projectId; return this; }
        public InvoiceDtoBuilder projectTitle(String projectTitle) { this.projectTitle = projectTitle; return this; }
        public InvoiceDtoBuilder clientName(String clientName) { this.clientName = clientName; return this; }
        public InvoiceDtoBuilder invoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; return this; }
        public InvoiceDtoBuilder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public InvoiceDtoBuilder cgst(BigDecimal cgst) { this.cgst = cgst; return this; }
        public InvoiceDtoBuilder sgst(BigDecimal sgst) { this.sgst = sgst; return this; }
        public InvoiceDtoBuilder igst(BigDecimal igst) { this.igst = igst; return this; }
        public InvoiceDtoBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public InvoiceDtoBuilder status(InvoiceStatus status) { this.status = status; return this; }
        public InvoiceDtoBuilder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public InvoiceDtoBuilder isRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; return this; }
        public InvoiceDtoBuilder lineItems(List<InvoiceLineItemDto> lineItems) { this.lineItems = lineItems; return this; }
        public InvoiceDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public InvoiceDto build() {
            return new InvoiceDto(id, projectId, projectTitle, clientName, invoiceNumber, subtotal, cgst, sgst, igst, totalAmount, status, dueDate, isRecurring, lineItems, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
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
    public List<InvoiceLineItemDto> getLineItems() { return lineItems; }
    public void setLineItems(List<InvoiceLineItemDto> lineItems) { this.lineItems = lineItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
