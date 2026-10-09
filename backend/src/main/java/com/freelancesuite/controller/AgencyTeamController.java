package com.freelancesuite.controller;

import com.freelancesuite.dto.InviteMemberRequest;
import com.freelancesuite.dto.TeamOverviewDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.AccountLifecycleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/agency/team")
@PreAuthorize("hasRole('OWNER')")
public class AgencyTeamController {
    private final AccountLifecycleService accountLifecycleService;
    public AgencyTeamController(AccountLifecycleService accountLifecycleService) {
        this.accountLifecycleService = accountLifecycleService;
    }

    @GetMapping
    public ResponseEntity<TeamOverviewDto> getTeam(@AuthenticationPrincipal UserPrincipal owner) {
        return ResponseEntity.ok(accountLifecycleService.getTeam(owner.getAgencyId()));
    }

    @PostMapping("/invitations")
    public ResponseEntity<Map<String, String>> invite(@Valid @RequestBody InviteMemberRequest request,
                                                       @AuthenticationPrincipal UserPrincipal owner) {
        accountLifecycleService.invite(request, owner);
        return ResponseEntity.accepted().body(Map.of("message", "Invitation email sent."));
    }

    @DeleteMapping("/invitations/{id}")
    public ResponseEntity<Void> revokeInvitation(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserPrincipal owner) {
        accountLifecycleService.revokeInvitation(id, owner);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/members/{id}/deactivate")
    public ResponseEntity<Void> deactivateMember(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserPrincipal owner) {
        accountLifecycleService.deactivateMember(id, owner);
        return ResponseEntity.noContent().build();
    }
}
