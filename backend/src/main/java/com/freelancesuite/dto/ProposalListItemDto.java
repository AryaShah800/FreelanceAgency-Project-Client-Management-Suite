package com.freelancesuite.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Agency-facing summary for managing published proposals. */
public record ProposalListItemDto(
        Long id,
        String clientName,
        String projectScope,
        BigDecimal estimatedBudget,
        boolean signed,
        String signatureName,
        LocalDateTime signedAt,
        boolean convertedToProject,
        Long createdProjectId,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean revoked,
        String shareToken) {
}
