package com.freelancesuite.controller;

import com.freelancesuite.dto.AiContractRequest;
import com.freelancesuite.service.AIService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final AIService aiService;

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate-contract")
    public ResponseEntity<Map<String, String>> generateContract(@Valid @RequestBody AiContractRequest request) {
        String result = aiService.generateProposalContract(request);
        return ResponseEntity.ok(Map.of("contract", result));
    }
}
