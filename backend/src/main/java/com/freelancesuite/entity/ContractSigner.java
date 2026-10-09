package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.SignerRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistence model for a person designated to sign a contract version.
 * Signing order and post-publication immutability require application-layer
 * enforcement and are not provided by this entity alone.
 */
@Entity
@Table(name = "contract_signers")
public class ContractSigner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_version_id", nullable = false)
    private ContractVersion contractVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignerRole signerRole;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String designation;
    private String organizationName;

    /** Execution order for sequential multi-party signing (1-based). */
    @Column(nullable = false)
    private Integer signingOrder = 1;

    private LocalDateTime createdAt;

    public ContractSigner() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public static ContractSignerBuilder builder() { return new ContractSignerBuilder(); }

    public static class ContractSignerBuilder {
        private final ContractSigner s = new ContractSigner();

        public ContractSignerBuilder contractVersion(ContractVersion v) { s.contractVersion = v; return this; }
        public ContractSignerBuilder signerRole(SignerRole v) { s.signerRole = v; return this; }
        public ContractSignerBuilder fullName(String v) { s.fullName = v; return this; }
        public ContractSignerBuilder email(String v) { s.email = v; return this; }
        public ContractSignerBuilder designation(String v) { s.designation = v; return this; }
        public ContractSignerBuilder organizationName(String v) { s.organizationName = v; return this; }
        public ContractSignerBuilder signingOrder(Integer v) { s.signingOrder = v; return this; }

        public ContractSigner build() { return s; }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ContractVersion getContractVersion() { return contractVersion; }
    public void setContractVersion(ContractVersion contractVersion) { this.contractVersion = contractVersion; }
    public SignerRole getSignerRole() { return signerRole; }
    public void setSignerRole(SignerRole signerRole) { this.signerRole = signerRole; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public Integer getSigningOrder() { return signingOrder; }
    public void setSigningOrder(Integer signingOrder) { this.signingOrder = signingOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
