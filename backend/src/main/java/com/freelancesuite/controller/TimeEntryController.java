package com.freelancesuite.controller;

import com.freelancesuite.dto.TimeEntryDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.TimeEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/time-entries")
@PreAuthorize("hasAnyRole('OWNER', 'MEMBER')")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @Autowired
    public TimeEntryController(TimeEntryService timeEntryService) {
        this.timeEntryService = timeEntryService;
    }

    @GetMapping("/active")
    public ResponseEntity<TimeEntryDto> getActiveTimer(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Optional<TimeEntryDto> active = timeEntryService.getActiveTimer(userPrincipal);
        return active.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/start")
    public ResponseEntity<TimeEntryDto> startTimer(
            @RequestParam Long taskId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(timeEntryService.startTimer(taskId, userPrincipal));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<TimeEntryDto> stopTimer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(timeEntryService.stopTimer(id, userPrincipal));
    }
}
