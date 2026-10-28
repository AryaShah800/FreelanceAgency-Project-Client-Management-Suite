package com.freelancesuite;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private AppUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void proposalListHandlesConcurrentReadStress() throws Exception {
        Account account = registerAgency("ProposalStress");
        int workers = 12;
        int requestsPerWorker = 40;
        CountDownLatch start = new CountDownLatch(1);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> requests = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                requests.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < requestsPerWorker; i++) {
                        long before = System.nanoTime();
                        int responseStatus = mockMvc.perform(get("/api/v1/proposals")
                                        .header("Authorization", account.bearer()))
                                .andReturn().getResponse().getStatus();
                        latencies.add(System.nanoTime() - before);
                        if (responseStatus != 200) {
                            throw new AssertionError("Expected HTTP 200 during read load, got " + responseStatus);
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> request : requests) request.get();
        } finally {
            executor.shutdownNow();
        }
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        long p95Millis = sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1) / 1_000_000;
        assertEquals(workers * requestsPerWorker, sorted.size());
        System.out.printf("Proposal read stress: %d requests, %d workers, all HTTP 200, p95 %d ms%n",
                sorted.size(), workers, p95Millis);
    }

    @Test
    void registrationLoginAndCurrentUserWork() throws Exception {
        String newOwnerEmail = "new-owner-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("agencyName", "Auth Agency", "ownerName", "Auth Owner",
                                "email", newOwnerEmail, "password", "StrongPass123!"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Check your email")));
        assertEquals(false, userRepository.findByEmail(newOwnerEmail).orElseThrow().isEmailVerified());

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
        long taskId = createTask(account, projectId);

        mockMvc.perform(get("/api/v1/clients").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Workflow Client"));
        mockMvc.perform(get("/api/v1/projects/{id}", projectId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Website delivery"));
        mockMvc.perform(get("/api/v1/tasks/project/{id}", projectId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Implement landing page"));
        mockMvc.perform(get("/api/v1/tasks").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignedToName").value("Workflow Owner"));

        mockMvc.perform(post("/api/v1/projects/{id}/expenses", projectId)
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("description", "Hosting", "amount", 500, "category", "Software"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500));
        mockMvc.perform(get("/api/v1/projects/{id}/expenses/profitability", projectId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(10000))
                .andExpect(jsonPath("$.totalExpenses").value(500));
        mockMvc.perform(get("/api/v1/projects/{id}/expenses", projectId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectTitle").value("Website delivery"));

        MvcResult timer = mockMvc.perform(post("/api/v1/time-entries/start").param("taskId", Long.toString(taskId))
                        .header("Authorization", auth))
                .andExpect(status().isOk()).andReturn();
        long timerId = body(timer).get("id").asLong();
        mockMvc.perform(get("/api/v1/time-entries/active").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskTitle").value("Implement landing page"))
                .andExpect(jsonPath("$.userName").value("Workflow Owner"));
        mockMvc.perform(post("/api/v1/time-entries/{id}/stop", timerId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(1));
        mockMvc.perform(get("/api/v1/projects/{id}/expenses/profitability", projectId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").isNumber());

        MvcResult invoice = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId, "lineItems", List.of(Map.of(
                                "description", "Design", "quantity", 2, "unitPrice", 1000))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(2000))
                .andExpect(jsonPath("$.totalAmount").value(2360))
                .andReturn();
        long invoiceId = body(invoice).get("id").asLong();
        mockMvc.perform(get("/api/v1/invoices").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lineItems[0].description").value("Design"));
        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectTitle").value("Website delivery"));
        mockMvc.perform(get("/api/v1/invoices/{id}/pdf", invoiceId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void draftInvoicesStayPrivateUntilSentAndCannotBePaidEarly() throws Exception {
        Account owner = registerAgency("InvoiceLifecycle");
        String clientEmail = "invoice-client-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
        long clientId = createClient(owner.bearer(), clientEmail);
        long projectId = createProject(owner.bearer(), clientId);
        MvcResult created = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", owner.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId, "lineItems", List.of(Map.of(
                                "description", "Synthetic test work", "quantity", 1, "unitPrice", 1000))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn();
        long invoiceId = body(created).get("id").asLong();
        Account client = createAgencyUserWithEmail(owner, Role.CLIENT, clientEmail, "Invoice Client");

        mockMvc.perform(get("/api/v1/invoices").header("Authorization", client.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId).header("Authorization", client.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/invoices/{id}/pdf", invoiceId).header("Authorization", client.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/payments/razorpay/create-order").param("invoiceId", Long.toString(invoiceId))
                        .header("Authorization", client.bearer()))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId).param("status", "PAID")
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/invoices/{id}/status", invoiceId).param("status", "SENT")
                        .header("Authorization", owner.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
        mockMvc.perform(get("/api/v1/invoices").header("Authorization", client.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    void proposalManagementIsAvailableToOwnersAndAdminsAndScopedToTheirAgency() throws Exception {
        Account owner = registerAgency("ProposalManager");
        MvcResult published = mockMvc.perform(post("/api/v1/proposals/publish")
                        .header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clientName", "Northstar Studio",
                                "clientEmail", "contact@northstar.example",
                                "projectScope", "Brand website delivery",
                                "deliverables", "Website and CMS",
                                "paymentTerms", "50% upfront",
                                "totalBudget", 25000,
                                "documentHtml", "<h1>Website proposal</h1><script>bad()</script><p onclick=bad()>Safe</p><a href='javascript:bad()'>link</a>"))))
                .andExpect(status().isOk()).andReturn();
        JsonNode publishedBody = body(published);

        mockMvc.perform(get("/api/v1/proposals").header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(publishedBody.get("id").asLong()))
                .andExpect(jsonPath("$[0].clientName").value("Northstar Studio"))
                .andExpect(jsonPath("$[0].signed").value(false))
                .andExpect(jsonPath("$[0].shareToken").value(publishedBody.get("shareToken").asText()));

        mockMvc.perform(get("/api/v1/public/portal/{token}", publishedBody.get("shareToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentHtml").value(org.hamcrest.Matchers.containsString("Website proposal")))
                .andExpect(jsonPath("$.documentHtml").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("script"))))
                .andExpect(jsonPath("$.documentHtml").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("onclick"))))
                .andExpect(jsonPath("$.documentHtml").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("javascript:"))));

        mockMvc.perform(post("/api/v1/public/portal/{token}/acceptance", publishedBody.get("shareToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("signerName", "Someone Else", "signerEmail", "other@example.test"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/proposals/{token}", publishedBody.get("shareToken").asText()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/proposals/{token}/sign", publishedBody.get("shareToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("signatureName", "Someone Else", "signerEmail", "contact@northstar.example"))))
                .andExpect(status().isForbidden());

        AppUser ownerUser = userRepository.findById(owner.userId()).orElseThrow();
        Account admin = createAgencyUser(ownerUser.getAgency(), Role.ADMIN, "proposal-admin");
        mockMvc.perform(get("/api/v1/proposals").header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
        mockMvc.perform(patch("/api/v1/proposals/{id}/revoke", publishedBody.get("id").asLong())
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/proposals").header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revoked").value(true));

        Account staff = createAgencyUser(ownerUser.getAgency(), Role.MEMBER, "proposal-staff");
        mockMvc.perform(get("/api/v1/proposals").header("Authorization", staff.bearer()))
                .andExpect(status().isForbidden());

        Account otherAgency = registerAgency("OtherProposalAgency");
        mockMvc.perform(get("/api/v1/proposals").header("Authorization", otherAgency.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void publicProposalAcceptancePdfAndProjectConversionShareThePublicPortalFlow() throws Exception {
        Account owner = registerAgency("ProposalAcceptance");
        MvcResult published = mockMvc.perform(post("/api/v1/proposals/publish")
                        .header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clientName", "Acceptance Client",
                                "clientEmail", "acceptance@client.example",
                                "projectScope", "Implement client portal launch",
                                "deliverables", "Portal and rollout support",
                                "paymentTerms", "50% deposit",
                                "totalBudget", 40000,
                                "documentHtml", "<h1>Client portal</h1>"))))
                .andExpect(status().isOk()).andReturn();
        String token = body(published).get("shareToken").asText();

        mockMvc.perform(post("/api/v1/public/portal/{token}/acceptance", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("signerName", "Client Contact", "signerEmail", "acceptance@client.example"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSigned").value(true))
                .andExpect(jsonPath("$.signatureName").value("Client Contact"));

        mockMvc.perform(get("/api/v1/public/portal/{token}/acceptance-record.pdf", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));

        mockMvc.perform(post("/api/v1/public/portal/{token}/convert", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").isNumber());
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
        long taskId = createTask(owner, projectId);
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
        Agency agency = agencyRepository.save(Agency.builder().name(label + " Agency").build());
        AppUser owner = AppUser.builder().agency(agency).name(label + " Owner").email(email)
                .passwordHash(passwordEncoder.encode("StrongPass123!")).role(Role.OWNER).build();
        owner.setEmailVerified(true);
        userRepository.save(owner);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "StrongPass123!"))))
                .andExpect(status().isOk()).andReturn();
        return new Account(email, body(result).get("token").asText(), body(result).get("userId").asLong());
    }

    private Account createAgencyUser(Agency agency, Role role, String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
        AppUser user = AppUser.builder().agency(agency).name(prefix).email(email)
                .passwordHash(passwordEncoder.encode("StrongPass123!")).role(role).build();
        user.setEmailVerified(true);
        userRepository.save(user);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "StrongPass123!"))))
                .andExpect(status().isOk()).andReturn();
        return new Account(email, body(result).get("token").asText(), body(result).get("userId").asLong());
    }

    private Account createAgencyUserWithEmail(Account owner, Role role, String email, String name) throws Exception {
        AppUser ownerUser = userRepository.findById(owner.userId()).orElseThrow();
        AppUser user = AppUser.builder().agency(ownerUser.getAgency()).name(name).email(email)
                .passwordHash(passwordEncoder.encode("StrongPass123!")).role(role).build();
        user.setEmailVerified(true);
        userRepository.save(user);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "StrongPass123!"))))
                .andExpect(status().isOk()).andReturn();
        return new Account(email, body(result).get("token").asText(), body(result).get("userId").asLong());
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

    private long createTask(Account account, long projectId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tasks").header("Authorization", account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("projectId", projectId, "assignedToId", account.userId(),
                                "title", "Implement landing page", "status", "TODO", "estimatedHours", 4))))
                .andExpect(status().isOk()).andReturn();
        return body(result).get("id").asLong();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record Account(String email, String token, long userId) {
        String bearer() { return "Bearer " + token; }
    }
}
