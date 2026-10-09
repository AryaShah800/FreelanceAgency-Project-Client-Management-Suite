package com.freelancesuite.service;

import com.freelancesuite.dto.PublicPortalProposalDto;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.Proposal;
import com.freelancesuite.entity.enums.DealStage;
import com.freelancesuite.repository.ClientRepository;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.repository.ProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProposalConversionServiceTest {

    private ProjectRepository projectRepository;
    private ClientRepository clientRepository;
    private InvoiceRepository invoiceRepository;
    private ProposalRepository proposalRepository;
    private AuditEventService auditEventService;

    private ProposalConversionService service;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        clientRepository = mock(ClientRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        proposalRepository = mock(ProposalRepository.class);
        auditEventService = mock(AuditEventService.class);

        service = new ProposalConversionService(
                projectRepository,
                clientRepository,
                invoiceRepository,
                proposalRepository,
                auditEventService
        );
    }

    private Proposal openProposal() {
        com.freelancesuite.entity.Agency dummyAgency = com.freelancesuite.entity.Agency.builder().id(99L).name("Test Agency").build();
        return Proposal.builder()
                .agency(dummyAgency)
                .shareToken("tok-123")
                .clientName("Acme Co")
                .projectScope("Build the thing")
                .deliverables("- Milestone 1\n- Milestone 2")
                .paymentTerms("50% upfront")
                .estimatedBudget(new BigDecimal("100000.00"))
                .expiresAt(LocalDateTime.now().plusDays(10))
                .isRevoked(false)
                .build();
    }

    @Test
    void getPublicProposalReturnsTheProposalWhenValid() {
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(openProposal()));

        PublicPortalProposalDto dto = service.getPublicProposal("tok-123");

        assertEquals("Acme Co", dto.getClientName());
        verify(auditEventService).logEvent(anyLong(), eq("Public Client"), eq("PROPOSAL_VIEWED"),
                argThat(details -> !details.contains("tok-123")));
    }

    @Test
    void getPublicProposalRejectsAnExpiredLink() {
        Proposal expired = openProposal();
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(expired));

        assertThrows(IllegalArgumentException.class, () -> service.getPublicProposal("tok-123"));
    }

    @Test
    void getPublicProposalRejectsARevokedLink() {
        Proposal revoked = openProposal();
        revoked.setRevoked(true);
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(revoked));

        assertThrows(IllegalArgumentException.class, () -> service.getPublicProposal("tok-123"));
    }

    @Test
    void getPublicProposalRejectsAnUnknownToken() {
        when(proposalRepository.findByShareToken("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getPublicProposal("missing"));
    }

    @Test
    void proposalAcceptanceComputesAHashAndAnAcceptancePdfThenPersists() {
        Proposal proposal = openProposal();
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(proposal));
        when(proposalRepository.saveAndFlush(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicPortalProposalDto dto = service.recordProposalAcceptance("tok-123", "Jane Doe");

        assertTrue(dto.getIsSigned());
        assertEquals("Jane Doe", dto.getSignatureName());
        assertNotNull(dto.getSignatureHash());
        assertEquals(64, dto.getSignatureHash().length(), "SHA-256 hex digest should be 64 characters");
        assertNotNull(proposal.getAcceptanceRecordPdf());
        assertTrue(proposal.getAcceptanceRecordPdf().length > 0);
        verify(proposalRepository).saveAndFlush(proposal);
        verify(auditEventService).logEvent(anyLong(), eq("Jane Doe (Client)"), eq("PROPOSAL_ACCEPTED"),
                argThat(details -> !details.contains("tok-123")));
    }

    @Test
    void getAcceptanceRecordPdfRejectsAnUnsignedProposal() {
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(openProposal()));

        assertThrows(IllegalArgumentException.class, () -> service.getAcceptanceRecordPdf("tok-123"));
    }

    @Test
    void getAcceptanceRecordPdfUsesTheSameRevocationCheckAsPublicProposalReads() {
        Proposal revoked = openProposal();
        revoked.setSigned(true);
        revoked.setAcceptanceRecordPdf(new byte[]{1, 2, 3});
        revoked.setRevoked(true);
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(revoked));

        assertThrows(IllegalArgumentException.class, () -> service.getAcceptanceRecordPdf("tok-123"));
    }

    @Test
    void getAcceptanceRecordPdfReturnsTheSavedSnapshotAfterAcceptance() {
        Proposal accepted = openProposal();
        accepted.setSigned(true);
        accepted.setExpiresAt(LocalDateTime.now().minusDays(1));
        accepted.setAcceptanceRecordPdf(new byte[]{1, 2, 3});
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(accepted));

        assertArrayEquals(new byte[]{1, 2, 3}, service.getAcceptanceRecordPdf("tok-123"));
    }

    @Test
    void convertProposalToProjectRequiresTheProposalToBeSignedFirst() {
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(openProposal()));

        assertThrows(IllegalStateException.class, () -> service.convertProposalToProject("tok-123"));
        verifyNoInteractions(projectRepository, invoiceRepository);
    }

    @Test
    void convertProposalToProjectCreatesAProjectAndDepositInvoiceForASignedProposal() {
        Proposal proposal = openProposal();
        proposal.setSigned(true);
        proposal.setSignatureName("Jane Doe");
        proposal.setSignedAt(LocalDateTime.now());

        Client existingClient = Client.builder()
                .companyName("Acme Co")
                .email("contact@acme.co")
                .dealStage(DealStage.PROPOSAL_SENT)
                .build();

        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(proposal));
        when(clientRepository.findByAgencyIdAndCompanyNameIgnoreCase(any(), eq("Acme Co"))).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(42L);
            return p;
        });
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(7L);
            return i;
        });
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.convertProposalToProject("tok-123");

        assertEquals(42L, result.get("projectId"));
        assertEquals(DealStage.WON, existingClient.getDealStage());
        assertTrue(proposal.isConvertedToProject());
        assertNotNull(proposal.getDepositInvoiceNumber());
        assertEquals(0, new BigDecimal("59000.00").compareTo(proposal.getDepositAmount()));

        verify(clientRepository, never()).save(argThat(c -> c != existingClient));
    }

    @Test
    void convertProposalToProjectRejectsConvertingTwice() {
        Proposal proposal = openProposal();
        proposal.setSigned(true);
        proposal.setConvertedToProject(true);
        when(proposalRepository.findByShareToken("tok-123")).thenReturn(Optional.of(proposal));

        assertThrows(IllegalStateException.class, () -> service.convertProposalToProject("tok-123"));
    }
}
