package com.freelancesuite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end HTTP tests for the principal agency workflow and critical access boundaries.
 * Each test registers its own agency so its data is isolated from every other test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void registrationLoginAndCurrentUserWork() throws Exception {
        Account account = registerAgency("Auth");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", account.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(account.email()))
                .andExpect(jsonPath("$.agencyId").isNumber());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", account.email(), "password", "StrongPass123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void validationAndAuthenticationRejectInvalidRequests() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agencyName\":\"\",\"ownerName\":\"\",\"email\":\"bad\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void ownerCanCompleteCoreAgencyWorkflow() throws Exception {
        Account account = registerAgency("Workflow");
        String auth = account.bearer();
        long clientId = createClient(auth, "workflow-client@example.test");
        long projectId = createProject(auth, clientId);
        long taskId = createTask(auth, projectId);

        mockMvc.perform(get("/api/v1/clients").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Workflow Client"));
        mockMvc.perform(get("/api/v1/projects/{id}", projectId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Website delivery"));
        mockMvc.perform(get("/api/v1/tasks/project/{id}", projectId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Implement landing page"));

        mockMvc.perform(post("/api/v1/projects/{id}/expenses", projectId)
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("description", "Hosting", "amount", 500, "category", "Software"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500));
        mockMvc.perform(get("/api/v1/projects/{id}/expenses/profitability", projectId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(10000))
                .andExpect(jsonPath("$.totalExpenses").value(500));

        MvcResult timer = mockMvc.perform(post("/api/v1/time-entries/start").param("taskId", Long.toString(taskId))
                        .header("Authorization", auth))
                .andExpect(status().isOk()).andReturn();
        long timerId = body(timer).get("id").asLong();
        mockMvc.perform(post("/api/v1/time-entries/{id}/stop", timerId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(1));

        MvcResult invoice = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId, "lineItems", List.of(Map.of(
                                "description", "Design", "quantity", 2, "unitPrice", 1000))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(2000))
                .andExpect(jsonPath("$.totalAmount").value(2360))
                .andReturn();
        long invoiceId = body(invoice).get("id").asLong();
        mockMvc.perform(get("/api/v1/invoices/{id}/pdf", invoiceId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void anotherAgencyCannotReadExpensesFromForeignProject() throws Exception {
        Account owner = registerAgency("Owner");
        long projectId = createProject(owner.bearer(), createClient(owner.bearer(), "owner-client@example.test"));
        mockMvc.perform(post("/api/v1/projects/{id}/expenses", projectId).header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("description", "Private cost", "amount", 99, "category", "Hosting"))))
                .andExpect(status().isOk());

        Account intruder = registerAgency("Intruder");
        mockMvc.perform(get("/api/v1/projects/{id}/expenses", projectId).header("Authorization", intruder.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherAgencyCannotStartTimerForForeignTask() throws Exception {
        Account owner = registerAgency("TimerOwner");
        long projectId = createProject(owner.bearer(), createClient(owner.bearer(), "timer-owner@example.test"));
        long taskId = createTask(owner.bearer(), projectId);
        Account intruder = registerAgency("TimerIntruder");

        mockMvc.perform(post("/api/v1/time-entries/start").param("taskId", Long.toString(taskId))
                        .header("Authorization", intruder.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicProposalMustBeSignedBeforeItCanBeConverted() throws Exception {
        mockMvc.perform(post("/api/v1/public/portal/demo-proposal-token-2026/convert"))
                .andExpect(status().isBadRequest());
    }

    private Account registerAgency(String label) throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String email = label.toLowerCase() + "-" + id + "@example.test";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "agencyName", label + " Agency", "ownerName", label + " Owner",
                                "email", email, "password", "StrongPass123!"))))
                .andExpect(status().isOk()).andReturn();
        return new Account(email, body(result).get("token").asText());
    }

    private long createClient(String auth, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/clients").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("companyName", "Workflow Client", "contactPerson", "Test Client", "email", email))))
                .andExpect(status().isOk()).andReturn();
        return body(result).get("id").asLong();
    }

    private long createProject(String auth, long clientId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("clientId", clientId, "title", "Website delivery", "description", "Deliver a website", "budget", 10000))))
                .andExpect(status().isOk()).andReturn();
        return body(result).get("id").asLong();
    }

    private long createTask(String auth, long projectId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tasks").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId, "title", "Implement landing page", "status", "TODO", "estimatedHours", 4))))
                .andExpect(status().isOk()).andReturn();
        return body(result).get("id").asLong();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record Account(String email, String token) {
        String bearer() { return "Bearer " + token; }
    }
}
