package com.freelancesuite.service;

import com.freelancesuite.dto.AiContractRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class ContractDraftTemplateTest {
    @ParameterizedTest
    @ValueSource(ints={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29})
    void preservesCommercialInputsAndLabelsFallback(int n) {
        var request = new AiContractRequest();
        request.setClientName("Synthetic Client " + n);
        request.setProjectScope("Deliver a scoped software service for the synthetic evaluation client.");
        request.setDeliverables("Source code and documentation " + n);
        request.setBudgetGuidance("INR " + (10000 + n * 2500));
        request.setPaymentTerms("50% on approval; 50% on acceptance");
        request.setTimeline("2027-01-10 to 2027-02-10");
        request.setGstStatus(n % 2 == 0 ? "GST excluded; confirm applicability" : "GST included; rate to be confirmed");
        request.setJurisdiction(n % 2 == 0 ? "Ahmedabad, Gujarat" : "Mumbai, Maharashtra");
        var service = new AIService(WebClient.builder(), new ObjectMapper());
        var result = service.generateProposalContract(request, (long) n);
        assertEquals("template_v2", result.getPromptVersion());
        assertTrue(result.getTitle().contains(request.getClientName()));
        assertEquals(9, result.getSections().size());
        String text = result.getSections().stream().map(x -> x.getContent()).reduce("", String::concat);
        for (String fact : new String[]{request.getBudgetGuidance(),request.getPaymentTerms(),request.getTimeline(),request.getGstStatus(),request.getJurisdiction()}) {
            assertTrue(text.contains(fact), "Missing supplied fact: " + fact);
        }
        assertTrue(result.getReviewNotes().contains("Local template; not an AI-generated result."));
    }

    @Test void escapesInputAndDoesNotInventMissingTerms() {
        var request = new AiContractRequest();
        request.setClientName("Synthetic Client");
        request.setProjectScope("<script>alert('x')</script> valid scope text");
        var result = new AIService(WebClient.builder(), new ObjectMapper()).generateProposalContract(request, 99L);
        String text = result.getSections().stream().map(x -> x.getContent()).reduce("", String::concat);
        assertFalse(text.contains("<script>"));
        assertTrue(text.contains("Not specified; confirmation required"));
        assertFalse(text.contains("4 to 6 weeks"));
        assertFalse(text.contains("40%"));
    }

    @Test void rejectsGeneratedDraftThatChangesSuppliedAmount() {
        var request = new AiContractRequest();
        request.setClientName("Synthetic Client");
        request.setProjectScope("Deliver a specified implementation and its technical documentation.");
        request.setBudgetGuidance("INR 25000");
        var service = new AIService(WebClient.builder(), new ObjectMapper());
        var draft = service.generateProposalContract(request, 100L);
        service.validateProposalOutput(draft, request);
        draft.getSections().stream().filter(s -> s.getHeading().equals("Payment Terms")).findFirst().orElseThrow()
                .setContent("<p>Budget guidance: INR 99999; payment schedule to be confirmed.</p>");
        assertThrows(IllegalArgumentException.class, () -> service.validateProposalOutput(draft, request));
    }
}
