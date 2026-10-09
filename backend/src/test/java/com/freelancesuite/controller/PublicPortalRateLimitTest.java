package com.freelancesuite.controller;

import com.freelancesuite.service.ProposalConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class PublicPortalRateLimitTest {

    private PublicPortalController controller;
    private ProposalConversionService service;

    @BeforeEach
    void setUp() {
        service = mock(ProposalConversionService.class);
        controller = new PublicPortalController(service);
    }

    @Test
    void testRateLimitEnforced() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");

        // 20 requests should pass
        for (int i = 0; i < 20; i++) {
            ResponseEntity<?> response = controller.getPublicProposal("tok-123", request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        // 21st request should fail
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            controller.getPublicProposal("tok-123", request);
        });
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }
}
