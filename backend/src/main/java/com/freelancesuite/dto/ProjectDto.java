package com.freelancesuite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ProjectDto {
    private Long id;

    @NotNull(message = "Client ID is required")
    private Long clientId;
    private String clientName;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Budget is required")
    private BigDecimal budget;

    private LocalDate deadline;
    private List<UserDto> teamMembers;
    private LocalDateTime createdAt;

    public ProjectDto() {}

    public ProjectDto(Long id, Long clientId, String clientName, String title, String description, BigDecimal budget, LocalDate deadline, List<UserDto> teamMembers, LocalDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.title = title;
        this.description = description;
        this.budget = budget;
        this.deadline = deadline;
        this.teamMembers = teamMembers;
        this.createdAt = createdAt;
    }

    public static ProjectDtoBuilder builder() { return new ProjectDtoBuilder(); }

    public static class ProjectDtoBuilder {
        private Long id;
        private Long clientId;
        private String clientName;
        private String title;
        private String description;
        private BigDecimal budget;
        private LocalDate deadline;
        private List<UserDto> teamMembers;
        private LocalDateTime createdAt;

        public ProjectDtoBuilder id(Long id) { this.id = id; return this; }
        public ProjectDtoBuilder clientId(Long clientId) { this.clientId = clientId; return this; }
        public ProjectDtoBuilder clientName(String clientName) { this.clientName = clientName; return this; }
        public ProjectDtoBuilder title(String title) { this.title = title; return this; }
        public ProjectDtoBuilder description(String description) { this.description = description; return this; }
        public ProjectDtoBuilder budget(BigDecimal budget) { this.budget = budget; return this; }
        public ProjectDtoBuilder deadline(LocalDate deadline) { this.deadline = deadline; return this; }
        public ProjectDtoBuilder teamMembers(List<UserDto> teamMembers) { this.teamMembers = teamMembers; return this; }
        public ProjectDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ProjectDto build() {
            return new ProjectDto(id, clientId, clientName, title, description, budget, deadline, teamMembers, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public List<UserDto> getTeamMembers() { return teamMembers; }
    public void setTeamMembers(List<UserDto> teamMembers) { this.teamMembers = teamMembers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
