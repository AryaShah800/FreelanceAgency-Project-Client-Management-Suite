package com.freelancesuite.controller;

import com.freelancesuite.dto.TaskDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getAllAgencyTasks(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(taskService.getAllAgencyTasks(userPrincipal.getAgencyId()));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDto>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody TaskDto dto) {
        return ResponseEntity.ok(taskService.createTask(dto, userPrincipal.getAgencyId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> updateTask(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody TaskDto dto) {
        return ResponseEntity.ok(taskService.updateTask(id, dto, userPrincipal.getAgencyId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        taskService.deleteTask(id, userPrincipal.getAgencyId());
        return ResponseEntity.noContent().build();
    }
}
