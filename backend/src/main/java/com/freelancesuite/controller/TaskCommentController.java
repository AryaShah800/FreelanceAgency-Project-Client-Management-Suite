package com.freelancesuite.controller;

import com.freelancesuite.dto.TaskCommentDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.TaskCommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
public class TaskCommentController {

    private final TaskCommentService commentService;

    @Autowired
    public TaskCommentController(TaskCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<List<TaskCommentDto>> getComments(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(commentService.getCommentsForTask(taskId, userPrincipal));
    }

    @PostMapping
    public ResponseEntity<TaskCommentDto> addComment(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }
        return ResponseEntity.ok(commentService.addComment(taskId, content, userPrincipal));
    }
}
