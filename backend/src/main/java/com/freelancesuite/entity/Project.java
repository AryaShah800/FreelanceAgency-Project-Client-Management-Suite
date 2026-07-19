package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal budget;

    private LocalDate deadline;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_members",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<AppUser> teamMembers = new ArrayList<>();

    private LocalDateTime createdAt;

    public Project() {}

    public Project(Long id, Client client, String title, String description, BigDecimal budget, LocalDate deadline, List<AppUser> teamMembers, LocalDateTime createdAt) {
        this.id = id;
        this.client = client;
        this.title = title;
        this.description = description;
        this.budget = budget;
        this.deadline = deadline;
        this.teamMembers = teamMembers != null ? teamMembers : new ArrayList<>();
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static ProjectBuilder builder() { return new ProjectBuilder(); }

    public static class ProjectBuilder {
        private Long id;
        private Client client;
        private String title;
        private String description;
        private BigDecimal budget;
        private LocalDate deadline;
        private List<AppUser> teamMembers = new ArrayList<>();
        private LocalDateTime createdAt;

        public ProjectBuilder id(Long id) { this.id = id; return this; }
        public ProjectBuilder client(Client client) { this.client = client; return this; }
        public ProjectBuilder title(String title) { this.title = title; return this; }
        public ProjectBuilder description(String description) { this.description = description; return this; }
        public ProjectBuilder budget(BigDecimal budget) { this.budget = budget; return this; }
        public ProjectBuilder deadline(LocalDate deadline) { this.deadline = deadline; return this; }
        public ProjectBuilder teamMembers(List<AppUser> teamMembers) { this.teamMembers = teamMembers; return this; }
        public ProjectBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Project build() {
            return new Project(id, client, title, description, budget, deadline, teamMembers, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public List<AppUser> getTeamMembers() { return teamMembers; }
    public void setTeamMembers(List<AppUser> teamMembers) { this.teamMembers = teamMembers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
