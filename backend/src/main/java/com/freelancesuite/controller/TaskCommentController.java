package com.freelancesuite.controller;

import com.freelancesuite.dto.TaskCommentDto;
import com.freelancesuite.dto.TaskCommentRequest;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.TaskCommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@PreAuthorize("hasAnyRole('OWNER', 'MEMBER', 'CLIENT')")
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
            @Valid @RequestBody TaskCommentRequest body,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(commentService.addComment(taskId, body.getContent(), userPrincipal));
    }
}
