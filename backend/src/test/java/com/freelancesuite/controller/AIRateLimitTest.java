package com.freelancesuite.controller;

import com.freelancesuite.dto.AiContractRequest;
import com.freelancesuite.dto.AiProposalResponse;
import com.freelancesuite.service.AIService;
import com.freelancesuite.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AIRateLimitTest {

    private AIController aiController;
    private AIService aiService;

    @BeforeEach
    void setUp() {
        aiService = mock(AIService.class);
        aiController = new AIController(aiService);
    }

    @Test
    void testRateLimitEnforced() {
        UserPrincipal principal = new UserPrincipal(1L, "owner@example.com", "owner@example.com", "pass", 10L, com.freelancesuite.entity.enums.Role.OWNER, java.util.List.of());
        AiContractRequest req = new AiContractRequest();

        when(aiService.generateProposalContract(any(), eq(10L))).thenReturn(new AiProposalResponse());

        // First request should succeed
        ResponseEntity<AiProposalResponse> response = aiController.generateContract(req, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Second request immediately should fail with 429
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            aiController.generateContract(req, principal);
        });
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }
}
