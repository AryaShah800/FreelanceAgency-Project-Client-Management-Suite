package com.freelancesuite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The demo profile must open onto a believable agency, not an empty shell. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoWorkspaceSeedTest {
    @Autowired private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper();
    private String bearer;

    @BeforeEach
    void login() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"demo@agency.test\",\"password\":\"DemoAccess2026!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        bearer = "Bearer " + json.readTree(body).get("token").asText();
    }

    private JsonNode fetch(String path) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", bearer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void pipelineHasEveryDealStage() throws Exception {
        JsonNode clients = fetch("/api/v1/clients");
        assertEquals(8, clients.size());
        Set<String> stages = new HashSet<>();
        clients.forEach(c -> stages.add(c.get("dealStage").asText()));
        assertEquals(Set.of("LEAD", "CONTACTED", "PROPOSAL_SENT", "WON", "LOST"), stages);
    }

    @Test
    void invoicesCoverEveryStatusAndBothGstModes() throws Exception {
        JsonNode invoices = fetch("/api/v1/invoices");
        Set<String> statuses = new HashSet<>();
        boolean intra = false, inter = false;
        for (JsonNode inv : invoices) {
            statuses.add(inv.get("status").asText());
            intra |= inv.get("cgst").decimalValue().signum() > 0 && inv.get("igst").decimalValue().signum() == 0;
            inter |= inv.get("igst").decimalValue().signum() > 0 && inv.get("cgst").decimalValue().signum() == 0;
            assertEquals(0, inv.get("subtotal").decimalValue().add(inv.get("cgst").decimalValue())
                    .add(inv.get("sgst").decimalValue()).add(inv.get("igst").decimalValue())
                    .compareTo(inv.get("totalAmount").decimalValue()), "GST must reconcile on " + inv.get("invoiceNumber"));
        }
        assertEquals(Set.of("PAID", "SENT", "OVERDUE", "DRAFT"), statuses);
        assertTrue(intra && inter, "demo shows CGST+SGST and IGST invoices");
        assertTrue(invoices.size() >= 15);
    }

    @Test
    void projectsHaveBelievableMargins() throws Exception {
        JsonNode projects = fetch("/api/v1/projects");
        assertEquals(12, projects.size());
        for (JsonNode p : projects) {
            JsonNode profit = fetch("/api/v1/projects/" + p.get("id").asLong() + "/expenses/profitability");
            double margin = profit.get("marginPercent").asDouble();
            assertTrue(margin > 15 && margin < 70, p.get("title").asText() + " margin " + margin);
        }
    }

    @Test
    void activityFeedAndTasksArePopulated() throws Exception {
        assertTrue(fetch("/api/v1/audit-events").size() >= 6);
        JsonNode tasks = fetch("/api/v1/tasks");
        Set<String> columns = new HashSet<>();
        tasks.forEach(t -> columns.add(t.get("status").asText()));
        assertEquals(Set.of("TODO", "IN_PROGRESS", "REVIEW", "DONE"), columns);
    }
}
