package com.freelancesuite;

import com.freelancesuite.dto.AiContractRequest;
import com.freelancesuite.service.AIService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoProfileIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AIService aiService;

    @Test
    void publicDemoOwnerCanSignInWithoutEmailProviderCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@agency.test","password":"DemoAccess2026!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.agencyName").value("Freelance Suite Demo"));
    }

    @Test
    void failedAttemptsOnSharedDemoAccountDoNotLockOutOtherRecruiters() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"demo@agency.test","password":"incorrect"}
                                    """));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@agency.test","password":"DemoAccess2026!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void proposalDraftFallsBackLocallyWithoutGeminiCredentials() {
        AiContractRequest request = new AiContractRequest();
        request.setClientName("Sample Client");
        request.setProjectScope("Build a responsive customer portal and secure REST API for sample business workflows.");
        request.setDeliverables("Portal interface, REST API, deployment notes");

        var proposal = aiService.generateProposalContract(request, 1L);

        assertNotNull(proposal.getTitle());
        assertFalse(proposal.getSections().isEmpty());
        assertTrue(proposal.getTitle().contains("Sample Client"));
    }
}
