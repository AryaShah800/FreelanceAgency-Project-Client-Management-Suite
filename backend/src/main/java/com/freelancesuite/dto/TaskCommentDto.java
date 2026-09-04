package com.freelancesuite.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class TaskCommentDto {
    private Long id;
    private Long taskId;
    private Long authorId;
    private String authorName;
    private String authorRole;

    @NotBlank(message = "Comment content cannot be blank")
    private String content;

    private LocalDateTime createdAt;

    public TaskCommentDto() {}

    public TaskCommentDto(Long id, Long taskId, Long authorId, String authorName, String authorRole, String content, LocalDateTime createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static TaskCommentDtoBuilder builder() { return new TaskCommentDtoBuilder(); }

    public static class TaskCommentDtoBuilder {
        private Long id;
        private Long taskId;
        private Long authorId;
        private String authorName;
        private String authorRole;
        private String content;
        private LocalDateTime createdAt;

        public TaskCommentDtoBuilder id(Long id) { this.id = id; return this; }
        public TaskCommentDtoBuilder taskId(Long taskId) { this.taskId = taskId; return this; }
        public TaskCommentDtoBuilder authorId(Long authorId) { this.authorId = authorId; return this; }
        public TaskCommentDtoBuilder authorName(String authorName) { this.authorName = authorName; return this; }
        public TaskCommentDtoBuilder authorRole(String authorRole) { this.authorRole = authorRole; return this; }
        public TaskCommentDtoBuilder content(String content) { this.content = content; return this; }
        public TaskCommentDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TaskCommentDto build() {
            return new TaskCommentDto(id, taskId, authorId, authorName, authorRole, content, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
