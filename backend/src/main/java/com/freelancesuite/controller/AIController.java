package com.freelancesuite.controller;

import com.freelancesuite.dto.AiContractRequest;
import com.freelancesuite.dto.AiProposalResponse;
import com.freelancesuite.service.AIService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class AIController {

    private final AIService aiService;
    private final com.github.benmanes.caffeine.cache.Cache<Long, Long> rateLimits =
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .expireAfterWrite(10, java.util.concurrent.TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();

    private final com.github.benmanes.caffeine.cache.Cache<Long, Integer> dailyCounts =
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .expireAfterWrite(1, java.util.concurrent.TimeUnit.DAYS)
            .maximumSize(1000)
            .build();

    @Value("${app.ai.daily-generation-limit:50}")
    private int dailyGenerationLimit = 50;

    private void enforceRateLimit(Long agencyId) {
        long now = System.currentTimeMillis();
        Long lastRequest = rateLimits.getIfPresent(agencyId);
        if (lastRequest != null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Please wait before generating another proposal.");
        }
        rateLimits.put(agencyId, now);

        Integer count = dailyCounts.asMap().compute(agencyId, (k, v) -> (v == null) ? 1 : v + 1);
        if (count > Math.max(1, dailyGenerationLimit)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Daily AI generation limit reached.");
        }
    }

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ai-generator")
    public ResponseEntity<AiProposalResponse> generateContract(@Valid @RequestBody AiContractRequest request, @org.springframework.security.core.annotation.AuthenticationPrincipal com.freelancesuite.security.UserPrincipal userPrincipal) {
        enforceRateLimit(userPrincipal.getAgencyId());
        return ResponseEntity.ok(aiService.generateProposalContract(request, userPrincipal.getAgencyId()));
    }
}
