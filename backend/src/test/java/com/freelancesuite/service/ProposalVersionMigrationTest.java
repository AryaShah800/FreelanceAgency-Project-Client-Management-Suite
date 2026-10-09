package com.freelancesuite.service;

import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.Proposal;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
public class ProposalVersionMigrationTest {

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private ProposalConversionService proposalConversionService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void testExistingRowVersionMigration() {
        Agency agency = agencyRepository.save(Agency.builder().name("Test Migration").build());

        // Hibernate 7 generates @Version columns as NOT NULL. Relax the H2 test
        // schema to emulate rows created by the legacy nullable schema.
        entityManager.createNativeQuery("ALTER TABLE proposals ALTER COLUMN version DROP NOT NULL").executeUpdate();

        // Insert using native query to simulate old rows without a version.
        entityManager.createNativeQuery(
                "INSERT INTO proposals (agency_id, share_token, client_name, estimated_budget, is_signed, is_converted_to_project, is_revoked, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)")
                .setParameter(1, agency.getId())
                .setParameter(2, "old-token-123")
                .setParameter(3, "Old Client")
                .setParameter(4, BigDecimal.TEN)
                .setParameter(5, false)
                .setParameter(6, false)
                .setParameter(7, false)
                .executeUpdate();

        // Simulate Flyway/Liquibase migration to initialize null versions before app startup
        entityManager.createNativeQuery("UPDATE proposals SET version = 0 WHERE version IS NULL").executeUpdate();

        // Fetch it
        Proposal p = proposalRepository.findByShareToken("old-token-123").orElseThrow();
        assertEquals(0, p.getVersion(), "Existing rows will have version 0 after migration");

        // Now attempt to sign it (which triggers an update)
        proposalConversionService.recordProposalAcceptance("old-token-123", "Old Client Signer");

        // Fetch again to verify version is now populated and incremented to 1
        Proposal updated = proposalRepository.findByShareToken("old-token-123").orElseThrow();
        assertNotNull(updated.getVersion(), "Version should be populated after first update");
        assertEquals(1, updated.getVersion());
    }
}
