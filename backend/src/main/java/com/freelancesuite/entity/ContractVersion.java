package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistence model for a proposed contract revision. The fields support storing
 * snapshots and digests, but this entity does not compute hashes or enforce
 * immutability; those guarantees require an application service and tests.
 */
@Entity
@Table(name = "contract_versions")
public class ContractVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(nullable = false)
    private Integer versionNumber;

    /**
     * The deterministic, canonicalized JSON representation of all contract
     * parameters at this version. Used for semantic integrity verification.
     */
    @Column(columnDefinition = "TEXT")
    private String canonicalJson;

    /**
     * Reserved for a future digest of {@link #canonicalJson}; not computed here.
     */
    @Column(length = 64)
    private String canonicalJsonHash;

    /** PDF bytes associated with this version, if a workflow stores them. */
    @Column(name = "pdf_binary")
    private byte[] pdfBinary;

    /**
     * Reserved for a future digest of {@link #pdfBinary}; not verified here.
     */
    @Column(length = 64)
    private String pdfSha256;

    /**
     * Lifecycle marker only; application-layer checks are needed to enforce locks.
     */
    @Column(nullable = false)
    private boolean isLocked = false;

    // --- AI-generated scope content (Tier 2) ---
    @Column(columnDefinition = "TEXT")
    private String projectSummary;

    @Column(columnDefinition = "TEXT")
    private String technicalStack;

    @Column(columnDefinition = "TEXT")
    private String deliverablesJson;

    @Column(columnDefinition = "TEXT")
    private String clientDependencies;

    @Column(columnDefinition = "TEXT")
    private String technicalExclusions;

    private LocalDateTime createdAt;
    private LocalDateTime lockedAt;

    public ContractVersion() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public static ContractVersionBuilder builder() { return new ContractVersionBuilder(); }

    public static class ContractVersionBuilder {
        private final ContractVersion v = new ContractVersion();

        public ContractVersionBuilder contract(Contract c) { v.contract = c; return this; }
        public ContractVersionBuilder versionNumber(Integer n) { v.versionNumber = n; return this; }
        public ContractVersionBuilder canonicalJson(String s) { v.canonicalJson = s; return this; }
        public ContractVersionBuilder canonicalJsonHash(String s) { v.canonicalJsonHash = s; return this; }
        public ContractVersionBuilder pdfBinary(byte[] b) { v.pdfBinary = b; return this; }
        public ContractVersionBuilder pdfSha256(String s) { v.pdfSha256 = s; return this; }
        public ContractVersionBuilder isLocked(boolean b) { v.isLocked = b; return this; }
        public ContractVersionBuilder projectSummary(String s) { v.projectSummary = s; return this; }
        public ContractVersionBuilder technicalStack(String s) { v.technicalStack = s; return this; }
        public ContractVersionBuilder deliverablesJson(String s) { v.deliverablesJson = s; return this; }
        public ContractVersionBuilder clientDependencies(String s) { v.clientDependencies = s; return this; }
        public ContractVersionBuilder technicalExclusions(String s) { v.technicalExclusions = s; return this; }

        public ContractVersion build() { return v; }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public String getCanonicalJson() { return canonicalJson; }
    public void setCanonicalJson(String canonicalJson) { this.canonicalJson = canonicalJson; }
    public String getCanonicalJsonHash() { return canonicalJsonHash; }
    public void setCanonicalJsonHash(String canonicalJsonHash) { this.canonicalJsonHash = canonicalJsonHash; }
    public byte[] getPdfBinary() { return pdfBinary; }
    public void setPdfBinary(byte[] pdfBinary) { this.pdfBinary = pdfBinary; }
    public String getPdfSha256() { return pdfSha256; }
    public void setPdfSha256(String pdfSha256) { this.pdfSha256 = pdfSha256; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
    public String getProjectSummary() { return projectSummary; }
    public void setProjectSummary(String projectSummary) { this.projectSummary = projectSummary; }
    public String getTechnicalStack() { return technicalStack; }
    public void setTechnicalStack(String technicalStack) { this.technicalStack = technicalStack; }
    public String getDeliverablesJson() { return deliverablesJson; }
    public void setDeliverablesJson(String deliverablesJson) { this.deliverablesJson = deliverablesJson; }
    public String getClientDependencies() { return clientDependencies; }
    public void setClientDependencies(String clientDependencies) { this.clientDependencies = clientDependencies; }
    public String getTechnicalExclusions() { return technicalExclusions; }
    public void setTechnicalExclusions(String technicalExclusions) { this.technicalExclusions = technicalExclusions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
}
