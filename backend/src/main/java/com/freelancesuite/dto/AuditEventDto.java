package com.freelancesuite.dto;

import java.time.LocalDateTime;

public class AuditEventDto {
    private Long id;
    private Long agencyId;
    private String actor;
    private String action;
    private String details;
    private LocalDateTime timestamp;

    public AuditEventDto() {}

    public AuditEventDto(Long id, Long agencyId, String actor, String action, String details, LocalDateTime timestamp) {
        this.id = id;
        this.agencyId = agencyId;
        this.actor = actor;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    public static AuditEventDtoBuilder builder() { return new AuditEventDtoBuilder(); }

    public static class AuditEventDtoBuilder {
        private Long id;
        private Long agencyId;
        private String actor;
        private String action;
        private String details;
        private LocalDateTime timestamp;

        public AuditEventDtoBuilder id(Long id) { this.id = id; return this; }
        public AuditEventDtoBuilder agencyId(Long agencyId) { this.agencyId = agencyId; return this; }
        public AuditEventDtoBuilder actor(String actor) { this.actor = actor; return this; }
        public AuditEventDtoBuilder action(String action) { this.action = action; return this; }
        public AuditEventDtoBuilder details(String details) { this.details = details; return this; }
        public AuditEventDtoBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AuditEventDto build() {
            return new AuditEventDto(id, agencyId, actor, action, details, timestamp);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAgencyId() { return agencyId; }
    public void setAgencyId(Long agencyId) { this.agencyId = agencyId; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
