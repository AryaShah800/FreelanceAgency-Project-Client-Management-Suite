package com.freelancesuite.dto;

import com.freelancesuite.entity.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class TaskDto {
    private Long id;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long assignedToId;
    private String assignedToName;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private TaskStatus status;
    private Double estimatedHours;
    private Double actualHours;
    private LocalDateTime createdAt;

    public TaskDto() {}

    public TaskDto(Long id, Long projectId, Long assignedToId, String assignedToName, String title, String description, TaskStatus status, Double estimatedHours, Double actualHours, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.assignedToId = assignedToId;
        this.assignedToName = assignedToName;
        this.title = title;
        this.description = description;
        this.status = status;
        this.estimatedHours = estimatedHours;
        this.actualHours = actualHours;
        this.createdAt = createdAt;
    }

    public static TaskDtoBuilder builder() { return new TaskDtoBuilder(); }

    public static class TaskDtoBuilder {
        private Long id;
        private Long projectId;
        private Long assignedToId;
        private String assignedToName;
        private String title;
        private String description;
        private TaskStatus status;
        private Double estimatedHours;
        private Double actualHours;
        private LocalDateTime createdAt;

        public TaskDtoBuilder id(Long id) { this.id = id; return this; }
        public TaskDtoBuilder projectId(Long projectId) { this.projectId = projectId; return this; }
        public TaskDtoBuilder assignedToId(Long assignedToId) { this.assignedToId = assignedToId; return this; }
        public TaskDtoBuilder assignedToName(String assignedToName) { this.assignedToName = assignedToName; return this; }
        public TaskDtoBuilder title(String title) { this.title = title; return this; }
        public TaskDtoBuilder description(String description) { this.description = description; return this; }
        public TaskDtoBuilder status(TaskStatus status) { this.status = status; return this; }
        public TaskDtoBuilder estimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; return this; }
        public TaskDtoBuilder actualHours(Double actualHours) { this.actualHours = actualHours; return this; }
        public TaskDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TaskDto build() {
            return new TaskDto(id, projectId, assignedToId, assignedToName, title, description, status, estimatedHours, actualHours, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }
    public Double getActualHours() { return actualHours; }
    public void setActualHours(Double actualHours) { this.actualHours = actualHours; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
