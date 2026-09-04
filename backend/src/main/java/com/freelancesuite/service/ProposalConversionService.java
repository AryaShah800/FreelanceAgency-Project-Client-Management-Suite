package com.freelancesuite.service;

import com.freelancesuite.dto.PublicPortalProposalDto;
import com.freelancesuite.entity.*;
import com.freelancesuite.entity.enums.DealStage;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.repository.*;
import com.freelancesuite.util.PdfGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProposalConversionService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProposalRepository proposalRepository;
    private final AuditEventService auditEventService;

    @Autowired
    public ProposalConversionService(ProjectRepository projectRepository, ClientRepository clientRepository,
                                      InvoiceRepository invoiceRepository, ProposalRepository proposalRepository,
                                      AuditEventService auditEventService) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
        this.proposalRepository = proposalRepository;
        this.auditEventService = auditEventService;
    }

    public PublicPortalProposalDto getPublicProposal(String shareToken) {
        Proposal proposal = requireOpenProposal(shareToken);

        auditEventService.logEvent(agencyIdOf(proposal), "Public Client", "PROPOSAL_VIEWED",
                "Client viewed public proposal via token: " + shareToken);

        return mapToDto(proposal);
    }

    @Transactional
    public PublicPortalProposalDto signPublicProposal(String shareToken, String signerName) {
        Proposal proposal = requireOpenProposal(shareToken);

        LocalDateTime signedAt = LocalDateTime.now();
        String hash = computeSignatureHash(proposal, signerName, signedAt);

        proposal.setSigned(true);
        proposal.setSignatureName(signerName);
        proposal.setSignedAt(signedAt);
        proposal.setSignatureHash(hash);

        // Generate a locked PDF snapshot now, once, so what's downloadable later
        // can never drift from what was actually agreed to at sign time.
        ByteArrayInputStream pdfStream = PdfGeneratorUtil.generateContractPdf(proposal);
        try {
            proposal.setContractPdf(pdfStream.readAllBytes());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate the signed contract PDF", ex);
        }

        proposal = proposalRepository.save(proposal);

        auditEventService.logEvent(agencyIdOf(proposal), signerName + " (Client)", "CONTRACT_SIGNED",
                "Proposal digitally signed via public portal token: " + shareToken + " (hash: " + hash + ")");

        return mapToDto(proposal);
    }

    /**
     * Returns the locked contract PDF generated at sign time. Only available
     * once the proposal has actually been signed.
     */
    public byte[] getSignedContractPdf(String shareToken) {
        Proposal proposal = requireOpenProposal(shareToken);
        if (!proposal.isSigned() || proposal.getContractPdf() == null) {
            throw new IllegalArgumentException("This proposal has not been signed yet");
        }
        return proposal.getContractPdf();
    }

    @Transactional
    public Map<String, Object> convertProposalToProject(String shareToken) {
        Proposal proposal = requireOpenProposal(shareToken);

        if (proposal.isConvertedToProject()) {
            throw new IllegalStateException("Proposal has already been converted to an active project.");
        }
        if (!proposal.isSigned()) {
            throw new IllegalStateException("Proposal must be signed before it can be converted to a project.");
        }

        // 1. Fetch or create client
        Client client = clientRepository.findAll().stream()
                .filter(c -> c.getCompanyName().equalsIgnoreCase(proposal.getClientName()))
                .findFirst()
                .orElseGet(() -> {
                    Client newC = Client.builder()
                            .agency(proposal.getAgency())
                            .companyName(proposal.getClientName())
                            .contactPerson(proposal.getSignatureName())
                            .email(slugify(proposal.getClientName()) + "@client.example")
                            .dealStage(DealStage.WON)
                            .build();
                    return clientRepository.save(newC);
                });

        client.setDealStage(DealStage.WON);
        clientRepository.save(client);

        // 2. Create Project Entity
        Project project = Project.builder()
                .client(client)
                .title(proposal.getClientName() + " - " + proposal.getProjectScope().substring(0, Math.min(30, proposal.getProjectScope().length())))
                .description(proposal.getProjectScope())
                .budget(proposal.getEstimatedBudget())
                .deadline(LocalDate.now().plusMonths(2))
                .build();

        project = projectRepository.save(project);

        // 3. Auto-generate 50% Deposit Invoice (INV-DEP-...)
        BigDecimal totalBudget = proposal.getEstimatedBudget();
        BigDecimal depositSubtotal = totalBudget.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cgst = depositSubtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = depositSubtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = depositSubtotal.add(cgst).add(sgst);

        InvoiceLineItem depositItem = InvoiceLineItem.builder()
                .description("50% Project Kickoff Deposit Invoice for " + project.getTitle())
                .quantity(1)
                .unitPrice(depositSubtotal)
                .amount(depositSubtotal)
                .build();

        String invNum = "INV-DEP-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Invoice depositInvoice = Invoice.builder()
                .project(project)
                .invoiceNumber(invNum)
                .subtotal(depositSubtotal)
                .cgst(cgst)
                .sgst(sgst)
                .igst(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .status(InvoiceStatus.SENT)
                .dueDate(LocalDate.now().plusDays(7))
                .isRecurring(false)
                .build();

        depositItem.setInvoice(depositInvoice);
        depositInvoice.setLineItems(List.of(depositItem));

        depositInvoice = invoiceRepository.save(depositInvoice);

        // 4. Update Proposal State
        proposal.setConvertedToProject(true);
        proposal.setCreatedProjectId(project.getId());
        proposal.setDepositInvoiceNumber(depositInvoice.getInvoiceNumber());
        proposal.setDepositAmount(totalAmount);

        proposalRepository.save(proposal);

        // 5. Log Audit Event
        Long agencyId = client.getAgency() != null ? client.getAgency().getId() : agencyIdOf(proposal);
        auditEventService.logEvent(agencyId, "Client Portal Auto-Converter", "PROJECT_CONVERTED",
                "Proposal converted to Project #" + project.getId() + " (" + project.getTitle() + "). 50% Deposit Invoice generated: " + invNum);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Proposal successfully converted to active Project and 50% Deposit Invoice generated!");
        result.put("projectId", project.getId());
        result.put("projectTitle", project.getTitle());
        result.put("depositInvoiceId", depositInvoice.getId());
        result.put("depositInvoiceNumber", depositInvoice.getInvoiceNumber());
        result.put("depositTotalAmount", totalAmount);

        return result;
    }

    private Proposal requireOpenProposal(String shareToken) {
        Proposal proposal = proposalRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired public proposal share token."));

        if (proposal.isRevoked() || (proposal.getExpiresAt() != null && LocalDateTime.now().isAfter(proposal.getExpiresAt()))) {
            throw new IllegalArgumentException("This public share link has expired or been revoked.");
        }
        return proposal;
    }

    private Long agencyIdOf(Proposal proposal) {
        return proposal.getAgency() != null ? proposal.getAgency().getId() : 1L;
    }

    private String computeSignatureHash(Proposal proposal, String signerName, LocalDateTime signedAt) {
        String canonical = String.join("|",
                nullToEmpty(proposal.getShareToken()),
                nullToEmpty(proposal.getClientName()),
                nullToEmpty(proposal.getProjectScope()),
                nullToEmpty(proposal.getDeliverables()),
                nullToEmpty(proposal.getPaymentTerms()),
                String.valueOf(proposal.getEstimatedBudget()),
                nullToEmpty(signerName),
                signedAt.toString());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not compute signature hash", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String slugify(String value) {
        String slug = value == null ? "client" : value.toLowerCase().replaceAll("[^a-z0-9]+", "");
        return slug.isBlank() ? "client" : slug;
    }

    private PublicPortalProposalDto mapToDto(Proposal proposal) {
        return PublicPortalProposalDto.builder()
                .shareToken(proposal.getShareToken())
                .clientName(proposal.getClientName())
                .projectScope(proposal.getProjectScope())
                .deliverables(proposal.getDeliverables())
                .paymentTerms(proposal.getPaymentTerms())
                .estimatedBudget(proposal.getEstimatedBudget())
                .isSigned(proposal.isSigned())
                .signatureName(proposal.getSignatureName())
                .signedAt(proposal.getSignedAt())
                .signatureHash(proposal.getSignatureHash())
                .isConvertedToProject(proposal.isConvertedToProject())
                .createdProjectId(proposal.getCreatedProjectId())
                .depositInvoiceNumber(proposal.getDepositInvoiceNumber())
                .depositAmount(proposal.getDepositAmount())
                .expiresAt(proposal.getExpiresAt())
                .isRevoked(proposal.isRevoked())
                .build();
    }
}
