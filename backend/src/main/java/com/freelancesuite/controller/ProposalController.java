package com.freelancesuite.controller;

import com.freelancesuite.dto.PublishProposalRequest;
import com.freelancesuite.dto.ProposalListItemDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.ProposalConversionService;
import com.freelancesuite.service.ProposalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/proposals")
public class ProposalController {

    private final ProposalService proposalService;
    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProposalListItemDto>> listProposals(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(proposalService.listForAgency(userPrincipal.getAgencyId()));
    }

    // Agency publishes the proposal and gets a share token
    @PostMapping("/publish")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> publishProposal(@Valid @RequestBody PublishProposalRequest req,
                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {
        var proposal = proposalService.publishProposal(req, userPrincipal.getAgencyId());
        return ResponseEntity.ok(Map.of("shareToken", proposal.getShareToken(), "id", proposal.getId()));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> revokeProposal(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        proposalService.revokeProposal(id, userPrincipal.getAgencyId());
        return ResponseEntity.noContent().build();
    }

}
