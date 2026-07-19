package com.freelancesuite.controller;

import com.freelancesuite.dto.ProjectDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAllProjects(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(projectService.getProjectsForUser(userPrincipal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProjectById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id, userPrincipal));
    }

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProjectDto dto) {
        return ResponseEntity.ok(projectService.createProject(dto, userPrincipal.getAgencyId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody ProjectDto dto) {
        return ResponseEntity.ok(projectService.updateProject(id, dto, userPrincipal.getAgencyId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        projectService.deleteProject(id, userPrincipal.getAgencyId());
        return ResponseEntity.noContent().build();
    }
}
