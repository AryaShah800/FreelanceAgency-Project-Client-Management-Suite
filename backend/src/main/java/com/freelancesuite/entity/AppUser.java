package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private Double hourlyRate = 0.0;

    private LocalDateTime createdAt;

    public AppUser() {}

    public AppUser(Long id, Agency agency, String name, String email, String passwordHash, Role role, Double hourlyRate, LocalDateTime createdAt) {
        this.id = id;
        this.agency = agency;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.hourlyRate = hourlyRate != null ? hourlyRate : 0.0;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static AppUserBuilder builder() { return new AppUserBuilder(); }

    public static class AppUserBuilder {
        private Long id;
        private Agency agency;
        private String name;
        private String email;
        private String passwordHash;
        private Role role;
        private Double hourlyRate = 0.0;
        private LocalDateTime createdAt;

        public AppUserBuilder id(Long id) { this.id = id; return this; }
        public AppUserBuilder agency(Agency agency) { this.agency = agency; return this; }
        public AppUserBuilder name(String name) { this.name = name; return this; }
        public AppUserBuilder email(String email) { this.email = email; return this; }
        public AppUserBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public AppUserBuilder role(Role role) { this.role = role; return this; }
        public AppUserBuilder hourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; return this; }
        public AppUserBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public AppUser build() {
            return new AppUser(id, agency, name, email, passwordHash, role, hourlyRate, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Agency getAgency() { return agency; }
    public void setAgency(Agency agency) { this.agency = agency; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
