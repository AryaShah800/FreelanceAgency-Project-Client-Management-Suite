package com.freelancesuite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiContractRequest {
    @NotBlank(message = "Client name is required")
    @Size(max = 160, message = "Client name must be 160 characters or fewer")
    private String clientName;

    @NotBlank(message = "Project scope is required")
    @Size(min = 30, max = 6000, message = "Project scope must be between 30 and 6000 characters")
    private String projectScope;

    @Size(max = 6000, message = "Deliverables must be 6000 characters or fewer")
    private String deliverables;
    @Size(max = 2000, message = "Payment terms must be 2000 characters or fewer")
    private String paymentTerms;
    @Size(max = 500, message = "Timeline must be 500 characters or fewer")
    private String timeline;
    @Size(max = 500, message = "Budget guidance must be 500 characters or fewer")
    private String budgetGuidance;

    public AiContractRequest() {}

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getProjectScope() { return projectScope; }
    public void setProjectScope(String projectScope) { this.projectScope = projectScope; }
    public String getDeliverables() { return deliverables; }
    public void setDeliverables(String deliverables) { this.deliverables = deliverables; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public String getTimeline() { return timeline; }
    public void setTimeline(String timeline) { this.timeline = timeline; }
    public String getBudgetGuidance() { return budgetGuidance; }
    public void setBudgetGuidance(String budgetGuidance) { this.budgetGuidance = budgetGuidance; }
}
