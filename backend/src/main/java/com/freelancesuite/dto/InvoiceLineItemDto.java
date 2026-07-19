package com.freelancesuite.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class InvoiceLineItemDto {
    private Long id;

    @NotBlank(message = "Item description is required")
    private String description;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;

    private BigDecimal amount;

    public InvoiceLineItemDto() {}

    public InvoiceLineItemDto(Long id, String description, Integer quantity, BigDecimal unitPrice, BigDecimal amount) {
        this.id = id;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    public static InvoiceLineItemDtoBuilder builder() { return new InvoiceLineItemDtoBuilder(); }

    public static class InvoiceLineItemDtoBuilder {
        private Long id;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;

        public InvoiceLineItemDtoBuilder id(Long id) { this.id = id; return this; }
        public InvoiceLineItemDtoBuilder description(String description) { this.description = description; return this; }
        public InvoiceLineItemDtoBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public InvoiceLineItemDtoBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public InvoiceLineItemDtoBuilder amount(BigDecimal amount) { this.amount = amount; return this; }

        public InvoiceLineItemDto build() {
            return new InvoiceLineItemDto(id, description, quantity, unitPrice, amount);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
