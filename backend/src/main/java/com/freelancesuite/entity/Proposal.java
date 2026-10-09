package com.freelancesuite.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Version
    private Integer version;

    @Column(nullable = false, unique = true)
    private String shareToken;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @Column(nullable = false)
    private String clientName;

    @Column(name = "client_email", length = 254)
    private String clientEmail;

    @Column(columnDefinition = "TEXT")
    private String projectScope;

    @Column(columnDefinition = "TEXT")
    private String deliverables;

    @Column(columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String documentHtml;

    @Column(columnDefinition = "TEXT")
    private String paymentMilestonesJson;

    private BigDecimal estimatedBudget;

    private boolean isSigned = false;
    private String signatureName;
    private LocalDateTime signedAt;

    /** SHA-256 hex digest computed over the proposal content + signer + timestamp at sign time. */
    private String signatureHash;

    /** Saved PDF snapshot of typed proposal acceptance details. Null until acceptance is recorded. */
    @JsonIgnore
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "contract_pdf", columnDefinition = "bytea")
    private byte[] acceptanceRecordPdf;

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
        public ProposalBuilder clientEmail(String v) { proposal.clientEmail = v; return this; }
        public ProposalBuilder projectScope(String v) { proposal.projectScope = v; return this; }
        public ProposalBuilder deliverables(String v) { proposal.deliverables = v; return this; }
        public ProposalBuilder paymentTerms(String v) { proposal.paymentTerms = v; return this; }
        public ProposalBuilder documentHtml(String v) { proposal.documentHtml = v; return this; }
        public ProposalBuilder paymentMilestonesJson(String v) { proposal.paymentMilestonesJson = v; return this; }
        public ProposalBuilder estimatedBudget(BigDecimal v) { proposal.estimatedBudget = v; return this; }
        public ProposalBuilder isSigned(boolean v) { proposal.isSigned = v; return this; }
        public ProposalBuilder expiresAt(LocalDateTime v) { proposal.expiresAt = v; return this; }
        public ProposalBuilder isRevoked(boolean v) { proposal.isRevoked = v; return this; }
        public ProposalBuilder isConvertedToProject(boolean v) { proposal.isConvertedToProject = v; return this; }

        public Proposal build() { return proposal; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public Agency getAgency() { return agency; }
    public void setAgency(Agency agency) { this.agency = agency; }
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
    public String getDocumentHtml() { return documentHtml; }
    public void setDocumentHtml(String documentHtml) { this.documentHtml = documentHtml; }
    public String getPaymentMilestonesJson() { return paymentMilestonesJson; }
    public void setPaymentMilestonesJson(String paymentMilestonesJson) { this.paymentMilestonesJson = paymentMilestonesJson; }
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
    public byte[] getAcceptanceRecordPdf() { return acceptanceRecordPdf; }
    public void setAcceptanceRecordPdf(byte[] acceptanceRecordPdf) { this.acceptanceRecordPdf = acceptanceRecordPdf; }
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
