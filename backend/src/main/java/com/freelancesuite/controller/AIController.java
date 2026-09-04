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
@PreAuthorize("hasAnyRole('OWNER', 'MEMBER')")
public class AIController {

    private final AIService aiService;

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ai-generator")
    public ResponseEntity<AiProposalResponse> generateContract(@Valid @RequestBody AiContractRequest request) {
        return ResponseEntity.ok(aiService.generateProposalContract(request));
    }
}
