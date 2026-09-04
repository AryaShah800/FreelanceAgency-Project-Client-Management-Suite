package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_comments")
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime createdAt;

    public TaskComment() {}

    public TaskComment(Long id, Task task, AppUser author, String content, LocalDateTime createdAt) {
        this.id = id;
        this.task = task;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static TaskCommentBuilder builder() { return new TaskCommentBuilder(); }

    public static class TaskCommentBuilder {
        private Long id;
        private Task task;
        private AppUser author;
        private String content;
        private LocalDateTime createdAt;

        public TaskCommentBuilder id(Long id) { this.id = id; return this; }
        public TaskCommentBuilder task(Task task) { this.task = task; return this; }
        public TaskCommentBuilder author(AppUser author) { this.author = author; return this; }
        public TaskCommentBuilder content(String content) { this.content = content; return this; }
        public TaskCommentBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TaskComment build() {
            return new TaskComment(id, task, author, content, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public AppUser getAuthor() { return author; }
    public void setAuthor(AppUser author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
