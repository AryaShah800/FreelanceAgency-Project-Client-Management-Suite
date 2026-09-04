package com.freelancesuite.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PublicPortalProposalDto {
    private String shareToken;
    private String clientName;
    private String projectScope;
    private String deliverables;
    private String paymentTerms;
    private BigDecimal estimatedBudget;
    private Boolean isSigned;
    private String signatureName;
    private LocalDateTime signedAt;
    private Boolean isConvertedToProject;
    private Long createdProjectId;
    private String depositInvoiceNumber;
    private BigDecimal depositAmount;
    private LocalDateTime expiresAt;
    private Boolean isRevoked;

    public PublicPortalProposalDto() {}

    public PublicPortalProposalDto(String shareToken, String clientName, String projectScope, String deliverables, String paymentTerms, BigDecimal estimatedBudget, Boolean isSigned, String signatureName, LocalDateTime signedAt, Boolean isConvertedToProject, Long createdProjectId, String depositInvoiceNumber, BigDecimal depositAmount, LocalDateTime expiresAt, Boolean isRevoked) {
        this.shareToken = shareToken;
        this.clientName = clientName;
        this.projectScope = projectScope;
        this.deliverables = deliverables;
        this.paymentTerms = paymentTerms;
        this.estimatedBudget = estimatedBudget;
        this.isSigned = isSigned;
        this.signatureName = signatureName;
        this.signedAt = signedAt;
        this.isConvertedToProject = isConvertedToProject;
        this.createdProjectId = createdProjectId;
        this.depositInvoiceNumber = depositInvoiceNumber;
        this.depositAmount = depositAmount;
        this.expiresAt = expiresAt;
        this.isRevoked = isRevoked;
    }

    public static PublicPortalProposalDtoBuilder builder() { return new PublicPortalProposalDtoBuilder(); }

    public static class PublicPortalProposalDtoBuilder {
        private String shareToken;
        private String clientName;
        private String projectScope;
        private String deliverables;
        private String paymentTerms;
        private BigDecimal estimatedBudget;
        private Boolean isSigned;
        private String signatureName;
        private LocalDateTime signedAt;
        private Boolean isConvertedToProject;
        private Long createdProjectId;
        private String depositInvoiceNumber;
        private BigDecimal depositAmount;
        private LocalDateTime expiresAt;
        private Boolean isRevoked;

        public PublicPortalProposalDtoBuilder shareToken(String shareToken) { this.shareToken = shareToken; return this; }
        public PublicPortalProposalDtoBuilder clientName(String clientName) { this.clientName = clientName; return this; }
        public PublicPortalProposalDtoBuilder projectScope(String projectScope) { this.projectScope = projectScope; return this; }
        public PublicPortalProposalDtoBuilder deliverables(String deliverables) { this.deliverables = deliverables; return this; }
        public PublicPortalProposalDtoBuilder paymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; return this; }
        public PublicPortalProposalDtoBuilder estimatedBudget(BigDecimal estimatedBudget) { this.estimatedBudget = estimatedBudget; return this; }
        public PublicPortalProposalDtoBuilder isSigned(Boolean isSigned) { this.isSigned = isSigned; return this; }
        public PublicPortalProposalDtoBuilder signatureName(String signatureName) { this.signatureName = signatureName; return this; }
        public PublicPortalProposalDtoBuilder signedAt(LocalDateTime signedAt) { this.signedAt = signedAt; return this; }
        public PublicPortalProposalDtoBuilder isConvertedToProject(Boolean isConvertedToProject) { this.isConvertedToProject = isConvertedToProject; return this; }
        public PublicPortalProposalDtoBuilder createdProjectId(Long createdProjectId) { this.createdProjectId = createdProjectId; return this; }
        public PublicPortalProposalDtoBuilder depositInvoiceNumber(String depositInvoiceNumber) { this.depositInvoiceNumber = depositInvoiceNumber; return this; }
        public PublicPortalProposalDtoBuilder depositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; return this; }
        public PublicPortalProposalDtoBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public PublicPortalProposalDtoBuilder isRevoked(Boolean isRevoked) { this.isRevoked = isRevoked; return this; }

        public PublicPortalProposalDto build() {
            return new PublicPortalProposalDto(shareToken, clientName, projectScope, deliverables, paymentTerms, estimatedBudget, isSigned, signatureName, signedAt, isConvertedToProject, createdProjectId, depositInvoiceNumber, depositAmount, expiresAt, isRevoked);
        }
    }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getProjectScope() { return projectScope; }
    public void setProjectScope(String projectScope) { this.projectScope = projectScope; }
    public String getDeliverables() { return deliverables; }
    public void setDeliverables(String deliverables) { this.deliverables = deliverables; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public BigDecimal getEstimatedBudget() { return estimatedBudget; }
    public void setEstimatedBudget(BigDecimal estimatedBudget) { this.estimatedBudget = estimatedBudget; }
    public Boolean getIsSigned() { return isSigned; }
    public void setIsSigned(Boolean isSigned) { this.isSigned = isSigned; }
    public String getSignatureName() { return signatureName; }
    public void setSignatureName(String signatureName) { this.signatureName = signatureName; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public Boolean getIsConvertedToProject() { return isConvertedToProject; }
    public void setIsConvertedToProject(Boolean isConvertedToProject) { this.isConvertedToProject = isConvertedToProject; }
    public Long getCreatedProjectId() { return createdProjectId; }
    public void setCreatedProjectId(Long createdProjectId) { this.createdProjectId = createdProjectId; }
    public String getDepositInvoiceNumber() { return depositInvoiceNumber; }
    public void setDepositInvoiceNumber(String depositInvoiceNumber) { this.depositInvoiceNumber = depositInvoiceNumber; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Boolean getIsRevoked() { return isRevoked; }
    public void setIsRevoked(Boolean isRevoked) { this.isRevoked = isRevoked; }
}
