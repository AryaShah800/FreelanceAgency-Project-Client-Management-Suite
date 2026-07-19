package com.freelancesuite.dto;

import jakarta.validation.constraints.NotBlank;

public class AiContractRequest {
    @NotBlank(message = "Client name is required")
    private String clientName;

    @NotBlank(message = "Project scope is required")
    private String projectScope;

    private String deliverables;
    private String paymentTerms;

    public AiContractRequest() {}

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getProjectScope() { return projectScope; }
    public void setProjectScope(String projectScope) { this.projectScope = projectScope; }
    public String getDeliverables() { return deliverables; }
    public void setDeliverables(String deliverables) { this.deliverables = deliverables; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
}
