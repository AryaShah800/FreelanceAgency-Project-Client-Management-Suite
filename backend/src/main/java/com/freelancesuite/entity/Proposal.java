package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A shareable client proposal, accessible via {@link #shareToken} through the
 * public portal.
 */
@Entity
@Table(name = "proposals")
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String shareToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @Column(nullable = false)
    private String clientName;

    @Column(columnDefinition = "TEXT")
    private String projectScope;

    @Column(columnDefinition = "TEXT")
    private String deliverables;

    @Column(columnDefinition = "TEXT")
    private String paymentTerms;

    private BigDecimal estimatedBudget;

    private boolean isSigned = false;
    private String signatureName;
    private LocalDateTime signedAt;

    /** SHA-256 hex digest computed over the proposal content + signer + timestamp at sign time. */
    private String signatureHash;

    /** Locked PDF snapshot of the signed contract, generated once at sign time. Null until signed. */
    @Column(name = "contract_pdf", columnDefinition = "VARBINARY")
    private byte[] contractPdf;

    private boolean isConvertedToProject = false;
    private Long createdProjectId;
    private String depositInvoiceNumber;
    private BigDecimal depositAmount;

    private LocalDateTime expiresAt;
    private boolean isRevoked = false;

    private LocalDateTime createdAt;

    public Proposal() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static ProposalBuilder builder() { return new ProposalBuilder(); }

    public static class ProposalBuilder {
        private final Proposal proposal = new Proposal();

        public ProposalBuilder shareToken(String v) { proposal.shareToken = v; return this; }
        public ProposalBuilder agency(Agency v) { proposal.agency = v; return this; }
        public ProposalBuilder clientName(String v) { proposal.clientName = v; return this; }
        public ProposalBuilder projectScope(String v) { proposal.projectScope = v; return this; }
        public ProposalBuilder deliverables(String v) { proposal.deliverables = v; return this; }
        public ProposalBuilder paymentTerms(String v) { proposal.paymentTerms = v; return this; }
        public ProposalBuilder estimatedBudget(BigDecimal v) { proposal.estimatedBudget = v; return this; }
        public ProposalBuilder isSigned(boolean v) { proposal.isSigned = v; return this; }
        public ProposalBuilder expiresAt(LocalDateTime v) { proposal.expiresAt = v; return this; }
        public ProposalBuilder isRevoked(boolean v) { proposal.isRevoked = v; return this; }
        public ProposalBuilder isConvertedToProject(boolean v) { proposal.isConvertedToProject = v; return this; }

        public Proposal build() { return proposal; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public Agency getAgency() { return agency; }
    public void setAgency(Agency agency) { this.agency = agency; }
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
    public boolean isSigned() { return isSigned; }
    public void setSigned(boolean signed) { isSigned = signed; }
    public String getSignatureName() { return signatureName; }
    public void setSignatureName(String signatureName) { this.signatureName = signatureName; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public String getSignatureHash() { return signatureHash; }
    public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
    public byte[] getContractPdf() { return contractPdf; }
    public void setContractPdf(byte[] contractPdf) { this.contractPdf = contractPdf; }
    public boolean isConvertedToProject() { return isConvertedToProject; }
    public void setConvertedToProject(boolean convertedToProject) { isConvertedToProject = convertedToProject; }
    public Long getCreatedProjectId() { return createdProjectId; }
    public void setCreatedProjectId(Long createdProjectId) { this.createdProjectId = createdProjectId; }
    public String getDepositInvoiceNumber() { return depositInvoiceNumber; }
    public void setDepositInvoiceNumber(String depositInvoiceNumber) { this.depositInvoiceNumber = depositInvoiceNumber; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return isRevoked; }
    public void setRevoked(boolean revoked) { this.isRevoked = revoked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
