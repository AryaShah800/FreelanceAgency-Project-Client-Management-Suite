package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence model for proposed changes to a contract. Approval and version
 * transition rules are not enforced by this entity.
 */
@Entity
@Table(name = "contract_amendments")
public class ContractAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_contract_id", nullable = false)
    private Contract parentContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_version_id", nullable = false)
    private ContractVersion newVersion;

    @Column(nullable = false, unique = true)
    private String changeRequestNumber;

    @Column(columnDefinition = "TEXT")
    private String scopeDelta;

    private BigDecimal costDelta;

    private Integer timelineDeltaDays;

    private String approvedBy;

    private LocalDateTime createdAt;

    public ContractAmendment() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public static ContractAmendmentBuilder builder() { return new ContractAmendmentBuilder(); }

    public static class ContractAmendmentBuilder {
        private final ContractAmendment a = new ContractAmendment();

        public ContractAmendmentBuilder parentContract(Contract v) { a.parentContract = v; return this; }
        public ContractAmendmentBuilder newVersion(ContractVersion v) { a.newVersion = v; return this; }
        public ContractAmendmentBuilder changeRequestNumber(String v) { a.changeRequestNumber = v; return this; }
        public ContractAmendmentBuilder scopeDelta(String v) { a.scopeDelta = v; return this; }
        public ContractAmendmentBuilder costDelta(BigDecimal v) { a.costDelta = v; return this; }
        public ContractAmendmentBuilder timelineDeltaDays(Integer v) { a.timelineDeltaDays = v; return this; }
        public ContractAmendmentBuilder approvedBy(String v) { a.approvedBy = v; return this; }

        public ContractAmendment build() { return a; }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Contract getParentContract() { return parentContract; }
    public void setParentContract(Contract parentContract) { this.parentContract = parentContract; }
    public ContractVersion getNewVersion() { return newVersion; }
    public void setNewVersion(ContractVersion newVersion) { this.newVersion = newVersion; }
    public String getChangeRequestNumber() { return changeRequestNumber; }
    public void setChangeRequestNumber(String changeRequestNumber) { this.changeRequestNumber = changeRequestNumber; }
    public String getScopeDelta() { return scopeDelta; }
    public void setScopeDelta(String scopeDelta) { this.scopeDelta = scopeDelta; }
    public BigDecimal getCostDelta() { return costDelta; }
    public void setCostDelta(BigDecimal costDelta) { this.costDelta = costDelta; }
    public Integer getTimelineDeltaDays() { return timelineDeltaDays; }
    public void setTimelineDeltaDays(Integer timelineDeltaDays) { this.timelineDeltaDays = timelineDeltaDays; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
