package com.freelancesuite.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class PublishProposalRequest {
    @NotBlank
    @Size(max = 200)
    private String clientName;
    @NotBlank
    @Email
    @Size(max = 254)
    private String clientEmail;
    @Size(max = 12000)
    private String projectScope;
    @Size(max = 24000)
    private String deliverables;
    @Size(max = 12000)
    private String paymentTerms;
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal totalBudget;
    @NotBlank
    @Size(max = 1000000)
    private String documentHtml;
    @Size(max = 64000)
    private String paymentMilestonesJson;

    public PublishProposalRequest() {}

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    public String getProjectScope() { return projectScope; }
    public void setProjectScope(String projectScope) { this.projectScope = projectScope; }
    public String getDeliverables() { return deliverables; }
    public void setDeliverables(String deliverables) { this.deliverables = deliverables; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }

    public String getDocumentHtml() { return documentHtml; }
    public void setDocumentHtml(String documentHtml) { this.documentHtml = documentHtml; }

    public String getPaymentMilestonesJson() { return paymentMilestonesJson; }
    public void setPaymentMilestonesJson(String paymentMilestonesJson) { this.paymentMilestonesJson = paymentMilestonesJson; }
}
