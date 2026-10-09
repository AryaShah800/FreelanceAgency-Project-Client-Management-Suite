package com.freelancesuite.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorsReturnGenericMessageWithoutExceptionDetails() {
        var response = handler.handleGlobalException(new RuntimeException("SQL secret-column-value"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred. Please try again or contact support.", response.getBody().get("message"));
        assertFalse(response.getBody().toString().contains("secret-column-value"));
    }

    @Test
    void upstreamServerErrorDoesNotReturnProviderReason() {
        var response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "provider credential rejected"));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("The service is temporarily unavailable. Please try again later.", response.getBody().get("message"));
        assertFalse(response.getBody().toString().contains("credential"));
    }

    @Test
    void accessDeniedDoesNotReturnInternalAuthorizationDetails() {
        var response = handler.handleAccessDeniedException(new AccessDeniedException("foreign agency 93"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You do not have permission to perform this action.", response.getBody().get("message"));
        assertFalse(response.getBody().toString().contains("agency 93"));
    }
}
