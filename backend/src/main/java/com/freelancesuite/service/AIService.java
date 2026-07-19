package com.freelancesuite.service;

import com.freelancesuite.dto.AiContractRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.api-url}")
    private String apiUrl;

    @Autowired
    public AIService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public String generateProposalContract(AiContractRequest request) {
        String prompt = String.format(
            "Draft a professional Freelance Service Agreement / Proposal for client '%s'.\n" +
            "Project Scope: %s\n" +
            "Key Deliverables: %s\n" +
            "Payment Terms: %s\n" +
            "Format cleanly in markdown with sections for Scope, Deliverables, Payment Terms, and Legal Sign-off.",
            request.getClientName(),
            request.getProjectScope(),
            request.getDeliverables() != null ? request.getDeliverables() : "As specified in scope",
            request.getPaymentTerms() != null ? request.getPaymentTerms() : "50% upfront, 50% upon completion"
        );

        if ("demo_key".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            return generateMockProposal(request);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
                )
            );

            Mono<Map> responseMono = webClientBuilder.build()
                .post()
                .uri(apiUrl + "?key=" + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);

            Map response = responseMono.block();
            if (response != null && response.containsKey("candidates")) {
                List candidates = (List) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map content = (Map) firstCandidate.get("content");
                    List parts = (List) content.get("parts");
                    Map firstPart = (Map) parts.get(0);
                    return (String) firstPart.get("text");
                }
            }
        } catch (Exception ex) {
            return generateMockProposal(request);
        }

        return generateMockProposal(request);
    }

    private String generateMockProposal(AiContractRequest request) {
        return String.format("""
            # FREELANCE SERVICE AGREEMENT & PROPOSAL
            
            **Client:** %s  
            **Date:** %s  
            
            ---
            
            ## 1. Project Scope
            %s
            
            ## 2. Key Deliverables
            %s
            
            ## 3. Payment & Commercial Terms
            %s
            
            ## 4. Intellectual Property & Confidentiality
            Upon receipt of full payment, all intellectual property rights for custom code and designs developed for this project are transferred exclusively to %s.
            
            ---
            **Client Signature:** ____________________  
            **Agency Representative:** ____________________
            """,
            request.getClientName(),
            java.time.LocalDate.now(),
            request.getProjectScope(),
            request.getDeliverables() != null ? request.getDeliverables() : "1. Design Mockups\n2. Web Application Code\n3. Deployment Setup",
            request.getPaymentTerms() != null ? request.getPaymentTerms() : "50% Advance Upon Signing, 50% Upon Final Milestone Acceptance",
            request.getClientName()
        );
    }
}
