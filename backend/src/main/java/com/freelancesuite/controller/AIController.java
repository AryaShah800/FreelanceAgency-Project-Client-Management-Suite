package com.freelancesuite.controller;

import com.freelancesuite.dto.AiContractRequest;
import com.freelancesuite.dto.AiProposalResponse;
import com.freelancesuite.service.AIService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class AIController {

    private final AIService aiService;
    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ai-generator")
    public ResponseEntity<AiProposalResponse> generateContract(@Valid @RequestBody AiContractRequest request, @org.springframework.security.core.annotation.AuthenticationPrincipal com.freelancesuite.security.UserPrincipal userPrincipal) {
        // AIService enforces the cooldown, the 10-minute window and the daily cap atomically.
        return ResponseEntity.ok(aiService.generateProposalContract(request, userPrincipal.getAgencyId()));
    }
}
