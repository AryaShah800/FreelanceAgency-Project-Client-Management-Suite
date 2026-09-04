package com.freelancesuite.service;

import com.freelancesuite.dto.AiContractRequest;
import com.freelancesuite.dto.AiProposalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
public class AIService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.api-url}")
    private String apiUrl;

    @Autowired
    public AIService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    public AiProposalResponse generateProposalContract(AiContractRequest request) {
        if ("demo_key".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI proposal drafting is not configured. Add GEMINI_API_KEY to enable it.");
        }
        String prompt = String.format(
            "You are a proposal-writing assistant for a professional digital agency. Draft an editable client proposal, not a contract. " +
            "Use only the supplied facts. Do not invent certifications, client facts, pricing, dates, guarantees, legal obligations, or signatures. " +
            "Use concise, clear business language. Include review notes for items the agency must confirm.\n\n" +
            "Client: %s\nProject scope: %s\nDeliverables: %s\nTimeline: %s\nBudget guidance: %s\nPayment terms: %s\n\n" +
            "Return JSON only, matching this schema: {title:string, executiveSummary:string, sections:[{heading:string,content:string}], reviewNotes:[string]}. " +
            "Sections must include: Objectives and scope; Deliverables; Timeline and milestones; Commercial terms; Assumptions and exclusions; Next steps. " +
            "Every section content must be ready for a human to edit and send after review.",
            request.getClientName(),
            request.getProjectScope(),
            request.getDeliverables() != null ? request.getDeliverables() : "As specified in scope",
            request.getTimeline() != null ? request.getTimeline() : "To be confirmed",
            request.getBudgetGuidance() != null ? request.getBudgetGuidance() : "To be confirmed",
            request.getPaymentTerms() != null ? request.getPaymentTerms() : "To be confirmed"
        );

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", java.util.List.of(Map.of("parts", java.util.List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json")
            );

            Map response = webClientBuilder.build()
                .post()
                .uri(apiUrl)
                .header("x-goog-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            if (response != null && response.containsKey("candidates")) {
                java.util.List candidates = (java.util.List) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map content = (Map) firstCandidate.get("content");
                    java.util.List parts = (java.util.List) content.get("parts");
                    Map firstPart = (Map) parts.get(0);
                    AiProposalResponse proposal = objectMapper.readValue((String) firstPart.get("text"), AiProposalResponse.class);
                    if (proposal.getSections() == null || proposal.getSections().isEmpty()) {
                        throw new IllegalStateException("The AI response did not contain proposal sections");
                    }
                    proposal.setGeneratedAt(java.time.LocalDateTime.now());
                    return proposal;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("AI proposal drafting is temporarily unavailable. Please try again shortly.", ex);
        }
        throw new IllegalStateException("The AI provider returned no proposal content");
    }
}
