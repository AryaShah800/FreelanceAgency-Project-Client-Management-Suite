package com.freelancesuite.dto;


import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExpenseDto {
    private Long id;
    // Taken from the URL path (/projects/{projectId}/expenses), not the body.
    private Long projectId;
    private String projectTitle;
    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must be 255 characters or fewer")
    private String description;
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Amount can have at most 2 decimal places")
    private BigDecimal amount;
    @NotBlank(message = "Category is required")
    @Size(max = 255, message = "Category must be 255 characters or fewer")
    private String category;
    private LocalDateTime createdAt;

    public ExpenseDto() {}

    public ExpenseDto(Long id, Long projectId, String projectTitle, String description, BigDecimal amount, String category, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.createdAt = createdAt;
    }

    public static ExpenseDtoBuilder builder() { return new ExpenseDtoBuilder(); }

    public static class ExpenseDtoBuilder {
        private Long id;
        private Long projectId;
        private String projectTitle;
        private String description;
        private BigDecimal amount;
        private String category;
        private LocalDateTime createdAt;

        public ExpenseDtoBuilder id(Long id) { this.id = id; return this; }
        public ExpenseDtoBuilder projectId(Long projectId) { this.projectId = projectId; return this; }
        public ExpenseDtoBuilder projectTitle(String projectTitle) { this.projectTitle = projectTitle; return this; }
        public ExpenseDtoBuilder description(String description) { this.description = description; return this; }
        public ExpenseDtoBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public ExpenseDtoBuilder category(String category) { this.category = category; return this; }
        public ExpenseDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ExpenseDto build() {
            return new ExpenseDto(id, projectId, projectTitle, description, amount, category, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
