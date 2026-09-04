package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(nullable = false)
    private String actor; // e.g. "Client (Sarah Jenkins)", "System Auto-Converter", "Alex Mercer"

    @Column(nullable = false)
    private String action; // e.g. "PROPOSAL_VIEWED", "CONTRACT_SIGNED", "DEPOSIT_PAID", "PROJECT_CONVERTED"

    @Column(columnDefinition = "TEXT")
    private String details;

    private LocalDateTime timestamp;

    public AuditEvent() {}

    public AuditEvent(Long id, Agency agency, String actor, String action, String details, LocalDateTime timestamp) {
        this.id = id;
        this.agency = agency;
        this.actor = actor;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public static AuditEventBuilder builder() { return new AuditEventBuilder(); }

    public static class AuditEventBuilder {
        private Long id;
        private Agency agency;
        private String actor;
        private String action;
        private String details;
        private LocalDateTime timestamp;

        public AuditEventBuilder id(Long id) { this.id = id; return this; }
        public AuditEventBuilder agency(Agency agency) { this.agency = agency; return this; }
        public AuditEventBuilder actor(String actor) { this.actor = actor; return this; }
        public AuditEventBuilder action(String action) { this.action = action; return this; }
        public AuditEventBuilder details(String details) { this.details = details; return this; }
        public AuditEventBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AuditEvent build() {
            return new AuditEvent(id, agency, actor, action, details, timestamp);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Agency getAgency() { return agency; }
    public void setAgency(Agency agency) { this.agency = agency; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
