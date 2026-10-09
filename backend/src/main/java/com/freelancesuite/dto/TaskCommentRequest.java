package com.freelancesuite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TaskCommentRequest {
    @NotBlank(message = "Comment content is required.")
    @Size(max = 5000, message = "Comment content must be 5000 characters or fewer.")
    private String content;

    public TaskCommentRequest() { }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
