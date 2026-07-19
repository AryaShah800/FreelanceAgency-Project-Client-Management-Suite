package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agencies")
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String gstin;

    private String subscriptionPlan = "PRO";

    private LocalDateTime createdAt;

    public Agency() {}

    public Agency(Long id, String name, String gstin, String subscriptionPlan, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.gstin = gstin;
        this.subscriptionPlan = subscriptionPlan != null ? subscriptionPlan : "PRO";
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static AgencyBuilder builder() {
        return new AgencyBuilder();
    }

    public static class AgencyBuilder {
        private Long id;
        private String name;
        private String gstin;
        private String subscriptionPlan = "PRO";
        private LocalDateTime createdAt;

        public AgencyBuilder id(Long id) { this.id = id; return this; }
        public AgencyBuilder name(String name) { this.name = name; return this; }
        public AgencyBuilder gstin(String gstin) { this.gstin = gstin; return this; }
        public AgencyBuilder subscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; return this; }
        public AgencyBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Agency build() {
            return new Agency(id, name, gstin, subscriptionPlan, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
