package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistence model for contract lifecycle audit data. Hash fields are storage
 * only: no service currently creates, validates, or protects an append-only
 * chain, and these records do not establish legal admissibility.
 */
@Entity
@Table(name = "contract_audit_events")
public class ContractAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /** Optional: links to a specific version if the event is version-scoped. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_version_id")
    private ContractVersion contractVersion;

    /**
     * Monotonically increasing sequence number within a contract's audit chain.
     * The first event for a contract has sequence_number = 1.
     */
    @Column(nullable = false)
    private Long sequenceNumber;

    @Column(nullable = false)
    private String eventType;

    /** Identifier of the actor who triggered this event (user email, "System", etc.). */
    @Column(nullable = false)
    private String actorId;

    /**
     * SHA-256 digest of the event-specific payload (e.g., the canonical JSON
     * that was frozen, or the consent statement that was recorded).
     */
    @Column(length = 64)
    private String payloadDigest;

    /** SHA-256 hash of the preceding event in this contract's audit chain. Null for the genesis event. */
    @Column(length = 64)
    private String prevEventHash;

    /**
     * SHA-256 hash of this event, computed as:
     * SHA-256(sequenceNumber|timestamp|eventType|actorId|payloadDigest|prevEventHash)
     */
    @Column(length = 64, nullable = false)
    private String eventHash;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public ContractAuditEvent() {}

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }

    public static ContractAuditEventBuilder builder() { return new ContractAuditEventBuilder(); }

    public static class ContractAuditEventBuilder {
        private final ContractAuditEvent e = new ContractAuditEvent();

        public ContractAuditEventBuilder contract(Contract v) { e.contract = v; return this; }
        public ContractAuditEventBuilder contractVersion(ContractVersion v) { e.contractVersion = v; return this; }
        public ContractAuditEventBuilder sequenceNumber(Long v) { e.sequenceNumber = v; return this; }
        public ContractAuditEventBuilder eventType(String v) { e.eventType = v; return this; }
        public ContractAuditEventBuilder actorId(String v) { e.actorId = v; return this; }
        public ContractAuditEventBuilder payloadDigest(String v) { e.payloadDigest = v; return this; }
        public ContractAuditEventBuilder prevEventHash(String v) { e.prevEventHash = v; return this; }
        public ContractAuditEventBuilder eventHash(String v) { e.eventHash = v; return this; }
        public ContractAuditEventBuilder timestamp(LocalDateTime v) { e.timestamp = v; return this; }

        public ContractAuditEvent build() { return e; }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public ContractVersion getContractVersion() { return contractVersion; }
    public void setContractVersion(ContractVersion contractVersion) { this.contractVersion = contractVersion; }
    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getPayloadDigest() { return payloadDigest; }
    public void setPayloadDigest(String payloadDigest) { this.payloadDigest = payloadDigest; }
    public String getPrevEventHash() { return prevEventHash; }
    public void setPrevEventHash(String prevEventHash) { this.prevEventHash = prevEventHash; }
    public String getEventHash() { return eventHash; }
    public void setEventHash(String eventHash) { this.eventHash = eventHash; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
