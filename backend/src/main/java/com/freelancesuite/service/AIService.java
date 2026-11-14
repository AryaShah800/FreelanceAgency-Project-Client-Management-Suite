package com.freelancesuite.service;

import com.freelancesuite.dto.AiContractRequest;
import com.freelancesuite.dto.AiProposalResponse;
import com.freelancesuite.dto.ProposalSection;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    @Value("${app.ai.max-output-tokens:4096}")
    private int configuredMaxOutputTokens = 4096;



    private final Cache<Long, AiUsageWindow> usageWindows = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(20))
            .build();
    private static final int MAX_AI_REQUESTS = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes

    @Autowired
    public AIService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    public AiProposalResponse generateProposalContract(AiContractRequest request, Long agencyId) {
        enforceRateLimit(agencyId);

        if (apiKey != null && !apiKey.isBlank() && !"demo_key".equals(apiKey)) {
            String systemPrompt = loadPrompt("contract_v2.txt");

            String userPrompt = String.format(
                "PROJECT PARAMETERS:\n" +
                "- Client: %s\n- Project scope: %s\n- Deliverables: %s\n- Timeline: %s\n- Budget guidance: %s\n- Payment terms: %s\n- GST status: %s\n- Jurisdiction: %s",
                request.getClientName(),
                request.getProjectScope(),
                request.getDeliverables() != null && !request.getDeliverables().isBlank() ? request.getDeliverables() : "Not specified; confirmation required",
                provided(request.getTimeline()), provided(request.getBudgetGuidance()), provided(request.getPaymentTerms()), provided(request.getGstStatus()), provided(request.getJurisdiction())
            );

            Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of("parts", java.util.List.of(Map.of("text", systemPrompt))),
                "contents", java.util.List.of(Map.of("parts", java.util.List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of(
                    "temperature", 0.2,
                    "responseMimeType", "application/json",
                    "maxOutputTokens", Math.max(128, Math.min(4096, configuredMaxOutputTokens))
                )
            );

            try {
                Map response = webClientBuilder.build()
                    .post()
                    .uri(apiUrl)
                    .header("x-goog-api-key", apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(25));

                if (response != null && response.containsKey("candidates")) {
                    java.util.List candidates = (java.util.List) response.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map firstCandidate = (Map) candidates.get(0);
                        Map content = (Map) firstCandidate.get("content");
                        if (content != null && content.containsKey("parts")) {
                            java.util.List parts = (java.util.List) content.get("parts");
                            if (!parts.isEmpty()) {
                                Map firstPart = (Map) parts.get(0);
                                String rawText = (String) firstPart.get("text");
                                String cleanedJson = extractJson(rawText);
                                AiProposalResponse proposal = objectMapper.readValue(cleanedJson, AiProposalResponse.class);

                                validateProposalOutput(proposal, request);

                                if (proposal.getSections() != null && !proposal.getSections().isEmpty()) {
                                    proposal.setGeneratedAt(LocalDateTime.now());
                                    proposal.setPromptVersion("contract_v2");
                                    log.info("Successfully generated and parsed AI proposal from Gemini.");
                                    return proposal;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // One upstream request per user action avoids duplicate billable calls on
                // ambiguous timeouts. Never log provider responses or client prompt data.
                log.warn("Gemini request failed ({}); returning the local proposal template.", ex.getClass().getSimpleName());
            }
            log.warn("Gemini response was unavailable or invalid; returning the local proposal template.");
        }

        return generateFallbackProposal(request);
    }

    void validateProposalOutput(AiProposalResponse proposal, AiContractRequest request) {
        if (proposal == null || proposal.getTitle() == null || proposal.getExecutiveSummary() == null
                || proposal.getSections() == null || proposal.getSections().isEmpty()
                || proposal.getSections().stream().anyMatch(x -> x == null || x.getHeading() == null || x.getContent() == null || x.getContent().isBlank())) {
            throw new IllegalArgumentException("Incomplete draft schema");
        }
        for (String heading : List.of("Scope", "Deliverables", "Payment Terms", "GST", "IP Ownership", "Confidentiality", "Termination", "Governing Law", "Timeline")) {
            if (proposal.getSections().stream().noneMatch(x -> x.getHeading().equalsIgnoreCase(heading))) {
                throw new IllegalArgumentException("Missing draft clause");
            }
        }
        String fullText = (proposal.getTitle() + " " + proposal.getExecutiveSummary() + " " +
            proposal.getSections().stream().map(s -> s.getContent()).reduce("", String::concat)).toLowerCase();

        String plainText = org.jsoup.Jsoup.parse(fullText).text();
        for (String fact : new String[]{request.getClientName(), request.getBudgetGuidance(), request.getPaymentTerms(),
                request.getTimeline(), request.getGstStatus(), request.getJurisdiction()}) {
            if (fact != null && !fact.isBlank() && !plainText.contains(fact.trim().toLowerCase())) {
                throw new IllegalArgumentException("A supplied commercial fact was changed or omitted");
            }
        }

        // 1. First-person words
        if (fullText.matches(".*\\b(we|our|my|i|us)\\b.*")) {
            throw new IllegalArgumentException("Validation failed: Contains first-person words.");
        }

        // 2. Absolute promises
        if (fullText.matches(".*\\b(100%|guaranteed|full|bug-free)\\b.*")) {
            throw new IllegalArgumentException("Validation failed: Contains absolute promises or liability traps.");
        }

        // 3. Hallucinations
        String reqScope = (request.getProjectScope() + " " + request.getDeliverables()).toLowerCase();
        if (fullText.contains("websocket") && !reqScope.contains("websocket")) {
            throw new IllegalArgumentException("Validation failed: Hallucinated 'WebSocket'.");
        }
        if (fullText.contains("mobile app") && !reqScope.contains("mobile") && !reqScope.contains("app")) {
            throw new IllegalArgumentException("Validation failed: Hallucinated 'mobile app'.");
        }

        // 4. Double punctuation
        if (fullText.contains("..") || fullText.contains(",,")) {
            throw new IllegalArgumentException("Validation failed: Contains double punctuation.");
        }
    }

    void enforceRateLimit(Long agencyId) {
        long now = System.currentTimeMillis();
        usageWindows.asMap().compute(agencyId, (key, current) -> {
            if (current == null || now - current.windowStartedAtMillis() >= RATE_LIMIT_WINDOW_MS) {
                return new AiUsageWindow(1, now);
            }
            if (current.count() >= MAX_AI_REQUESTS) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                        "AI Generation rate limit exceeded (Max 10 per 10 minutes). Try again later.");
            }
            return new AiUsageWindow(current.count() + 1, current.windowStartedAtMillis());
        });
    }

    private record AiUsageWindow(int count, long windowStartedAtMillis) { }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) return "{}";
        String cleaned = text.trim();
        // Strip markdown code fences (```json ... ``` or ``` ... ```)
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
        }

        // Extract outer JSON structure bounded by { and }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }
        return cleaned;
    }

    private static String provided(String value) {
        return value == null || value.isBlank() ? "Not specified; confirmation required" : value.trim();
    }

    private String loadPrompt(String name) {
        try (var stream = new org.springframework.core.io.ClassPathResource("prompts/" + name).getInputStream()) {
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Draft prompt resource unavailable", ex);
        }
    }

    private AiProposalResponse generateFallbackProposal(AiContractRequest request) {
        AiProposalResponse response = new AiProposalResponse();
        response.setTitle("Services agreement review draft for " + provided(request.getClientName()));
        response.setExecutiveSummary("Review draft only. Commercial and legal terms require the parties' approval before signing.");
        List<ProposalSection> sections = new ArrayList<>();
        String[][] fields = {{"Scope", request.getProjectScope()}, {"Deliverables", request.getDeliverables()},
                {"Payment Terms", "Budget guidance: " + provided(request.getBudgetGuidance()) + "; Payment schedule: " + provided(request.getPaymentTerms())},
                {"GST", request.getGstStatus()}, {"Timeline", request.getTimeline()}, {"Governing Law", request.getJurisdiction()}};
        for (String[] field : fields) {
            sections.add(new ProposalSection(field[0], "<p>" + org.springframework.web.util.HtmlUtils.htmlEscape(provided(field[1])) + "</p>"));
        }
        sections.add(new ProposalSection("IP Ownership", "<p>Ownership, licensing and transfer conditions require the parties' approval.</p>"));
        sections.add(new ProposalSection("Confidentiality", "<p>Protected information, permitted disclosures and confidentiality duration require the parties' approval.</p>"));
        sections.add(new ProposalSection("Termination", "<p>Notice, grounds, fees and handover on termination require the parties' approval.</p>"));
        response.setSections(sections);
        response.setReviewNotes(List.of("Local template; not an AI-generated result.", "Confirm all unspecified terms and obtain appropriate legal review before signing."));
        response.setGeneratedAt(LocalDateTime.now());
        response.setPromptVersion("template_v2");
        return response;
    }
}
