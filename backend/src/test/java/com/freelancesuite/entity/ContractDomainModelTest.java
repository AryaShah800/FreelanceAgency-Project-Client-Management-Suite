package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying the new CLM domain model entity builders, relationships,
 * and default state behavior.
 */
class ContractDomainModelTest {

    @Test
    void contract_builderSetsAllFields() {
        Agency agency = Agency.builder().name("Test Agency").gstin("27AAACA0000A1Z5")
                .pan("AAACA0000A").stateCode("27").build();
        Client client = Client.builder().agency(agency).companyName("Test Client")
                .email("test@client.com").stateCode("29").build();
        Proposal proposal = Proposal.builder().shareToken("tok-1").agency(agency)
                .clientName("Test Client").build();

        Contract contract = Contract.builder()
                .agency(agency)
                .client(client)
                .proposal(proposal)
                .contractNumber("AGR-2026-0001")
                .status(ContractStatus.DRAFT)
                .stampingReference("NESL-1234567")
                .shareToken("contract-token-xyz")
                .build();

        assertEquals("AGR-2026-0001", contract.getContractNumber());
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertEquals("NESL-1234567", contract.getStampingReference());
        assertEquals("contract-token-xyz", contract.getShareToken());
        assertSame(agency, contract.getAgency());
        assertSame(client, contract.getClient());
        assertSame(proposal, contract.getProposal());
    }

    @Test
    void contract_defaultStatusIsDraft() {
        Contract contract = Contract.builder()
                .contractNumber("AGR-TEST")
                .agency(Agency.builder().name("A").build())
                .client(Client.builder().companyName("C").email("c@c.com").agency(Agency.builder().name("A").build()).build())
                .build();
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
    }

    @Test
    void contractVersion_builderSetsLockingFields() {
        Contract contract = Contract.builder().contractNumber("AGR-1")
                .agency(Agency.builder().name("A").build())
                .client(Client.builder().companyName("C").email("c@c.com").agency(Agency.builder().name("A").build()).build())
                .build();

        ContractVersion version = ContractVersion.builder()
                .contract(contract)
                .versionNumber(1)
                .canonicalJson("{\"scope\":\"test\"}")
                .canonicalJsonHash("abc123hash")
                .pdfSha256("def456hash")
                .isLocked(false)
                .projectSummary("Build a banking portal")
                .technicalStack("[\"Java\",\"React\"]")
                .build();

        assertEquals(1, version.getVersionNumber());
        assertFalse(version.isLocked());
        assertEquals("{\"scope\":\"test\"}", version.getCanonicalJson());
        assertEquals("abc123hash", version.getCanonicalJsonHash());
        assertSame(contract, version.getContract());
    }

    @Test
    void contractVersion_lockIsIrreversibleInDomainLogic() {
        ContractVersion version = ContractVersion.builder()
                .contract(Contract.builder().contractNumber("AGR-1")
                        .agency(Agency.builder().name("A").build())
                        .client(Client.builder().companyName("C").email("c@c.com").agency(Agency.builder().name("A").build()).build())
                        .build())
                .versionNumber(1)
                .isLocked(false)
                .build();

        assertFalse(version.isLocked());
        version.setLocked(true);
        version.setLockedAt(LocalDateTime.now());
        assertTrue(version.isLocked());
        assertNotNull(version.getLockedAt());
    }

    @Test
    void contractSigner_builderSetsMultiPartyFields() {
        ContractSigner agencySigner = ContractSigner.builder()
                .signerRole(SignerRole.AGENCY)
                .fullName("Alex Mercer")
                .email("alex@agency.com")
                .designation("CEO")
                .organizationName("Apex Digital Solutions")
                .signingOrder(1)
                .build();

        ContractSigner clientSigner = ContractSigner.builder()
                .signerRole(SignerRole.CLIENT)
                .fullName("Sarah Jenkins")
                .email("sarah@fintech.io")
                .designation("CTO")
                .organizationName("FinTech Innovations")
                .signingOrder(2)
                .build();

        assertEquals(SignerRole.AGENCY, agencySigner.getSignerRole());
        assertEquals(SignerRole.CLIENT, clientSigner.getSignerRole());
        assertEquals(1, agencySigner.getSigningOrder());
        assertEquals(2, clientSigner.getSigningOrder());
    }

    @Test
    void signatureEvent_capturesBsaTelemetry() {
        LocalDateTime signedAt = LocalDateTime.of(2026, 10, 1, 14, 30, 0);
        SignatureEvent event = SignatureEvent.builder()
                .signedAt(signedAt)
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .authMethod(AuthMethod.MAGIC_LINK_OTP)
                .consentStatement("I, Sarah Jenkins, hereby acknowledge...")
                .signatureType(SignatureType.CLICKWRAP_E_SIGN)
                .otpTokenDigest("sha256-of-otp-token")
                .build();

        assertEquals("192.168.1.100", event.getIpAddress());
        assertEquals(AuthMethod.MAGIC_LINK_OTP, event.getAuthMethod());
        assertEquals(SignatureType.CLICKWRAP_E_SIGN, event.getSignatureType());
        assertNotNull(event.getConsentStatement());
        assertEquals(signedAt, event.getSignedAt());
    }

    @Test
    void contractAuditEvent_hashChainFieldsPresent() {
        ContractAuditEvent event = ContractAuditEvent.builder()
                .sequenceNumber(1L)
                .eventType("CONTRACT_CREATED")
                .actorId("alex@agency.com")
                .payloadDigest("payload-sha256-hex")
                .prevEventHash(null) // genesis event
                .eventHash("computed-event-hash-hex")
                .timestamp(LocalDateTime.now())
                .build();

        assertEquals(1L, event.getSequenceNumber());
        assertEquals("CONTRACT_CREATED", event.getEventType());
        assertNull(event.getPrevEventHash()); // genesis
        assertNotNull(event.getEventHash());
    }

    @Test
    void contractAmendment_tracksDelta() {
        ContractAmendment amendment = ContractAmendment.builder()
                .changeRequestNumber("CR-2026-001")
                .scopeDelta("Added API v2 endpoints")
                .costDelta(new BigDecimal("50000.00"))
                .timelineDeltaDays(14)
                .approvedBy("alex@agency.com")
                .build();

        assertEquals("CR-2026-001", amendment.getChangeRequestNumber());
        assertEquals(new BigDecimal("50000.00"), amendment.getCostDelta());
        assertEquals(14, amendment.getTimelineDeltaDays());
    }

    @Test
    void agency_enrichedFieldsAreAccessible() {
        Agency agency = Agency.builder()
                .name("Apex Digital Solutions")
                .gstin("27AAACA0000A1Z5")
                .pan("AAACA0000A")
                .registeredAddress("301 Techno Tower, Andheri East, Mumbai")
                .stateCode("27")
                .bankName("HDFC Bank")
                .bankAccountNumber("50100000000001")
                .bankIfsc("HDFC0000001")
                .build();

        assertEquals("AAACA0000A", agency.getPan());
        assertEquals("27", agency.getStateCode());
        assertEquals("HDFC Bank", agency.getBankName());
        assertEquals("HDFC0000001", agency.getBankIfsc());
    }

    @Test
    void client_enrichedFieldsAreAccessible() {
        Client client = Client.builder()
                .companyName("FinTech Innovations")
                .email("sarah@fintech.io")
                .agency(Agency.builder().name("A").build())
                .gstin("27BBBCC1111B1Z2")
                .pan("BBBCC1111B")
                .registeredAddress("Unit 5A, BKC Complex, Mumbai")
                .stateCode("27")
                .build();

        assertEquals("BBBCC1111B", client.getPan());
        assertEquals("27", client.getStateCode());
        assertEquals("Unit 5A, BKC Complex, Mumbai", client.getRegisteredAddress());
    }

    @Test
    void sacCode_enumCarriesCodeAndDescription() {
        assertEquals("998314", SacCode.SAC_998314.getCode());
        assertEquals("IT Design & Development", SacCode.SAC_998314.getDescription());
        assertEquals("998315", SacCode.SAC_998315.getCode());
        assertEquals("998313", SacCode.SAC_998313.getCode());
    }

    @Test
    void contractStatus_enumHasAllStates() {
        ContractStatus[] statuses = ContractStatus.values();
        assertEquals(7, statuses.length);
        assertNotNull(ContractStatus.valueOf("DRAFT"));
        assertNotNull(ContractStatus.valueOf("PENDING_REVIEW"));
        assertNotNull(ContractStatus.valueOf("PENDING_SIGNATURE"));
        assertNotNull(ContractStatus.valueOf("ACTIVE"));
        assertNotNull(ContractStatus.valueOf("AMENDED"));
        assertNotNull(ContractStatus.valueOf("TERMINATED"));
        assertNotNull(ContractStatus.valueOf("REJECTED"));
    }
}
