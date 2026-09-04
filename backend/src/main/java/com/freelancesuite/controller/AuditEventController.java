package com.freelancesuite.controller;

import com.freelancesuite.dto.AuditEventDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.AuditEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
@PreAuthorize("hasAnyRole('OWNER', 'MEMBER')")
public class AuditEventController {

    private final AuditEventService auditEventService;

    @Autowired
    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping
    public ResponseEntity<List<AuditEventDto>> getAuditFeed(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(auditEventService.getAuditFeedForAgency(userPrincipal.getAgencyId()));
    }
}
