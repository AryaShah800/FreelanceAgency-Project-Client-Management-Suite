package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.ContractStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistence model for an agency agreement associated with a proposal and client.
 * Lifecycle transitions, signing, and version immutability are not enforced by
 * this entity; they require an application service before these records are used
 * as executed agreements.
 */
@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, unique = true)
    private String contractNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.DRAFT;

    /** Points to the currently active/latest ContractVersion.id. */
    private Long currentVersionId;

    /** Optional reference supplied by an external stamping provider; none is integrated. */
    private String stampingReference;

    /** Secure, unguessable token for the public signing portal. */
    @Column(unique = true)
    private String shareToken;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Contract() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static ContractBuilder builder() { return new ContractBuilder(); }

    public static class ContractBuilder {
        private final Contract c = new Contract();

        public ContractBuilder proposal(Proposal v) { c.proposal = v; return this; }
        public ContractBuilder agency(Agency v) { c.agency = v; return this; }
        public ContractBuilder client(Client v) { c.client = v; return this; }
        public ContractBuilder contractNumber(String v) { c.contractNumber = v; return this; }
        public ContractBuilder status(ContractStatus v) { c.status = v; return this; }
        public ContractBuilder currentVersionId(Long v) { c.currentVersionId = v; return this; }
        public ContractBuilder stampingReference(String v) { c.stampingReference = v; return this; }
        public ContractBuilder shareToken(String v) { c.shareToken = v; return this; }

        public Contract build() { return c; }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Proposal getProposal() { return proposal; }
    public void setProposal(Proposal proposal) { this.proposal = proposal; }
    public Agency getAgency() { return agency; }
    public void setAgency(Agency agency) { this.agency = agency; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }
    public Long getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }
    public String getStampingReference() { return stampingReference; }
    public void setStampingReference(String stampingReference) { this.stampingReference = stampingReference; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
