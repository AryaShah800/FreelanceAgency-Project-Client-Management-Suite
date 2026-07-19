package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.TaskStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private AppUser assignedTo;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    private Double estimatedHours = 0.0;
    private Double actualHours = 0.0;
    private LocalDateTime createdAt;

    public Task() {}

    public Task(Long id, Project project, AppUser assignedTo, String title, String description, TaskStatus status, Double estimatedHours, Double actualHours, LocalDateTime createdAt) {
        this.id = id;
        this.project = project;
        this.assignedTo = assignedTo;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : TaskStatus.TODO;
        this.estimatedHours = estimatedHours != null ? estimatedHours : 0.0;
        this.actualHours = actualHours != null ? actualHours : 0.0;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static TaskBuilder builder() { return new TaskBuilder(); }

    public static class TaskBuilder {
        private Long id;
        private Project project;
        private AppUser assignedTo;
        private String title;
        private String description;
        private TaskStatus status = TaskStatus.TODO;
        private Double estimatedHours = 0.0;
        private Double actualHours = 0.0;
        private LocalDateTime createdAt;

        public TaskBuilder id(Long id) { this.id = id; return this; }
        public TaskBuilder project(Project project) { this.project = project; return this; }
        public TaskBuilder assignedTo(AppUser assignedTo) { this.assignedTo = assignedTo; return this; }
        public TaskBuilder title(String title) { this.title = title; return this; }
        public TaskBuilder description(String description) { this.description = description; return this; }
        public TaskBuilder status(TaskStatus status) { this.status = status; return this; }
        public TaskBuilder estimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; return this; }
        public TaskBuilder actualHours(Double actualHours) { this.actualHours = actualHours; return this; }
        public TaskBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Task build() {
            return new Task(id, project, assignedTo, title, description, status, estimatedHours, actualHours, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public AppUser getAssignedTo() { return assignedTo; }
    public void setAssignedTo(AppUser assignedTo) { this.assignedTo = assignedTo; }
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
