package com.freelancesuite.service;

import com.freelancesuite.dto.PublicPortalProposalDto;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Autowired
    public ProposalConversionService(ProjectRepository projectRepository, ClientRepository clientRepository,
                                      InvoiceRepository invoiceRepository, ProposalRepository proposalRepository,
                                      AuditEventService auditEventService, ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
        this.proposalRepository = proposalRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    public ProposalConversionService(ProjectRepository projectRepository, ClientRepository clientRepository,
                                     InvoiceRepository invoiceRepository, ProposalRepository proposalRepository,
                                     AuditEventService auditEventService) {
        this(projectRepository, clientRepository, invoiceRepository, proposalRepository,
                auditEventService, new ObjectMapper());
    }

    @Transactional
    public PublicPortalProposalDto getPublicProposal(String shareToken) {
        Proposal proposal = requireOpenProposal(shareToken);

        auditEventService.logEvent(agencyIdOf(proposal), "Public Client", "PROPOSAL_VIEWED",
                "Client viewed a public proposal.");

        return mapToDto(proposal);
    }

    @Transactional
    public PublicPortalProposalDto recordProposalAcceptance(String shareToken, String signerName) {
        return recordProposalAcceptance(shareToken, signerName, null);
    }

    @Transactional
    public PublicPortalProposalDto recordProposalAcceptance(String shareToken, String signerName, String signerEmail) {
        Proposal proposal = requireOpenProposal(shareToken);

        if (proposal.isSigned()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Proposal has already been signed");
        }

        LocalDateTime signedAt = LocalDateTime.now();
        String normalizedEmail = signerEmail == null ? null : signerEmail.trim().toLowerCase(Locale.ROOT);
        if (proposal.getClientEmail() != null && (normalizedEmail == null
                || !proposal.getClientEmail().equalsIgnoreCase(normalizedEmail))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Use the contact email specified on the proposal.");
        }
        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            ensureClientRecord(proposal, signerName, normalizedEmail);
        }
        String hash = computeSignatureHash(proposal, signerName, normalizedEmail, signedAt);

        proposal.setSigned(true);
        proposal.setSignatureName(signerName);
        proposal.setSignedAt(signedAt);
        proposal.setSignatureHash(hash);

        // Save the acceptance record once so the downloaded PDF cannot drift
        // from the proposal content that was displayed when acceptance occurred.
        ByteArrayInputStream pdfStream = PdfGeneratorUtil.generateAcceptanceRecordPdf(proposal);
        try {
            proposal.setAcceptanceRecordPdf(pdfStream.readAllBytes());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate the proposal acceptance record PDF", ex);
        }

        try {
            proposal = proposalRepository.saveAndFlush(proposal);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Proposal has already been signed");
        }

        auditEventService.logEvent(agencyIdOf(proposal), signerName + " (Client)", "PROPOSAL_ACCEPTED",
                "Typed proposal acceptance recorded through the public portal (content hash: " + hash + ").");

        return mapToDto(proposal);
    }

    /**
     * Returns the saved typed-acceptance record. The shared visibility check
     * prevents the PDF endpoint from bypassing revocation or proposal expiry.
     */
    @Transactional(readOnly = true)
    public byte[] getAcceptanceRecordPdf(String shareToken) {
        Proposal proposal = requireReadableProposal(shareToken);
        if (!proposal.isSigned() || proposal.getAcceptanceRecordPdf() == null) {
            throw new IllegalArgumentException("This proposal has no acceptance record yet.");
        }
        return proposal.getAcceptanceRecordPdf();
    }

    @Transactional
    public Map<String, Object> convertProposalToProject(String shareToken) {
        Proposal proposal = requireReadableProposal(shareToken);

        if (proposal.isConvertedToProject()) {
            throw new IllegalStateException("Proposal has already been converted to an active project.");
        }
        if (!proposal.isSigned()) {
            throw new IllegalStateException("Proposal acceptance must be recorded before project conversion.");
        }

        // 1. Fetch or create client
        Long agencyId = agencyIdOf(proposal);
        Client client = clientRepository.findByAgencyIdAndCompanyNameIgnoreCase(agencyId, proposal.getClientName())
                .orElseThrow(() -> new IllegalStateException(
                        "Client contact details were not captured. Re-sign the proposal with a valid email address."));

        client.setDealStage(DealStage.WON);
        clientRepository.save(client);

        // 2. Create Project Entity
        Project project = Project.builder()
                .client(client)
                .title(projectTitle(proposal))
                .description(proposal.getProjectScope() == null ? "" : proposal.getProjectScope())
                .budget(proposal.getEstimatedBudget())
                .deadline(LocalDate.now().plusMonths(2))
                .build();

        project = projectRepository.save(project);

        // Create the agreed milestone invoices. Legacy proposals with no stored
        // schedule retain the original 50% deposit behavior.
        List<Invoice> milestoneInvoices = createMilestoneInvoices(proposal, project);
        Invoice firstInvoice = milestoneInvoices.get(0);

        // 4. Update Proposal State
        proposal.setConvertedToProject(true);
        proposal.setCreatedProjectId(project.getId());
        proposal.setDepositInvoiceNumber(firstInvoice.getInvoiceNumber());
        proposal.setDepositAmount(firstInvoice.getTotalAmount());

        proposalRepository.save(proposal);

        // 5. Log Audit Event
        Long finalAgencyId = client.getAgency() != null ? client.getAgency().getId() : agencyIdOf(proposal);
        auditEventService.logEvent(finalAgencyId, "Client Portal Auto-Converter", "PROJECT_CONVERTED",
                "Proposal converted to Project #" + project.getId() + " (" + project.getTitle() + "). " +
                        milestoneInvoices.size() + " milestone invoice(s) generated.");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Proposal converted to an active project and milestone invoices were generated.");
        result.put("projectId", project.getId());
        result.put("projectTitle", project.getTitle());
        result.put("depositInvoiceId", firstInvoice.getId());
        result.put("depositInvoiceNumber", firstInvoice.getInvoiceNumber());
        result.put("depositTotalAmount", firstInvoice.getTotalAmount());
        result.put("invoiceCount", milestoneInvoices.size());

        return result;
    }

    private String projectTitle(Proposal proposal) {
        String scope = proposal.getProjectScope() == null ? "Project" : proposal.getProjectScope().trim();
        String suffix = scope.isEmpty() ? "Project" : scope.substring(0, Math.min(30, scope.length()));
        return proposal.getClientName() + " - " + suffix;
    }

    private List<Invoice> createMilestoneInvoices(Proposal proposal, Project project) {
        if (proposal.getPaymentMilestonesJson() == null || proposal.getPaymentMilestonesJson().isBlank()) {
            return List.of(createLegacyDepositInvoice(proposal, project));
        }

        try {
            List<Map<String, Object>> milestones = objectMapper.readValue(
                    proposal.getPaymentMilestonesJson(), new TypeReference<>() {});
            if (milestones.isEmpty()) {
                throw new IllegalArgumentException("The proposal has no payment milestones.");
            }

            BigDecimal scheduledTotal = BigDecimal.ZERO;
            List<Invoice> invoices = new ArrayList<>();
            int index = 1;
            for (Map<String, Object> milestone : milestones) {
                BigDecimal subtotal = requiredMoney(milestone.get("invoiceSubtotal"), "invoiceSubtotal");
                BigDecimal cgst = moneyOrZero(milestone.get("cgst"));
                BigDecimal sgst = moneyOrZero(milestone.get("sgst"));
                BigDecimal igst = moneyOrZero(milestone.get("igst"));
                BigDecimal total = requiredMoney(milestone.get("invoiceTotal"), "invoiceTotal");
                if (subtotal.signum() <= 0 || total.signum() <= 0 ||
                        subtotal.add(cgst).add(sgst).add(igst).compareTo(total) != 0) {
                    throw new IllegalArgumentException("Payment milestone amounts do not reconcile.");
                }
                scheduledTotal = scheduledTotal.add(total);
                String label = String.valueOf(milestone.getOrDefault("label", "Milestone " + index));
                Object dueDateValue = milestone.get("dueDateIso");
                LocalDate dueDate = dueDateValue == null ? LocalDate.now().plusDays(7) : LocalDate.parse(dueDateValue.toString());

                Invoice invoice = Invoice.builder()
                        .project(project)
                        .invoiceNumber("INV-" + project.getId() + "-" + index++)
                        .subtotal(subtotal)
                        .cgst(cgst)
                        .sgst(sgst)
                        .igst(igst)
                        .totalAmount(total)
                        .status(InvoiceStatus.SENT)
                        .dueDate(dueDate)
                        .isRecurring(false)
                        .build();
                InvoiceLineItem item = InvoiceLineItem.builder()
                        .description(label)
                        .quantity(1)
                        .unitPrice(subtotal)
                        .amount(subtotal)
                        .invoice(invoice)
                        .build();
                invoice.setLineItems(List.of(item));
                invoices.add(invoiceRepository.save(invoice));
            }
            if (proposal.getEstimatedBudget() == null || scheduledTotal.compareTo(proposal.getEstimatedBudget()) != 0) {
                throw new IllegalArgumentException("Payment milestones must add up to the proposal total.");
            }
            return invoices;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("The proposal payment schedule is invalid; no project was converted.", ex);
        }
    }

    private Invoice createLegacyDepositInvoice(Proposal proposal, Project project) {
        BigDecimal depositSubtotal = proposal.getEstimatedBudget()
                .multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cgst = depositSubtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = depositSubtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = depositSubtotal.add(cgst).add(sgst);
        Invoice invoice = Invoice.builder()
                .project(project)
                .invoiceNumber("INV-DEP-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .subtotal(depositSubtotal)
                .cgst(cgst)
                .sgst(sgst)
                .igst(BigDecimal.ZERO)
                .totalAmount(total)
                .status(InvoiceStatus.SENT)
                .dueDate(LocalDate.now().plusDays(7))
                .isRecurring(false)
                .build();
        InvoiceLineItem item = InvoiceLineItem.builder()
                .description("50% Project Kickoff Deposit Invoice for " + project.getTitle())
                .quantity(1)
                .unitPrice(depositSubtotal)
                .amount(depositSubtotal)
                .invoice(invoice)
                .build();
        invoice.setLineItems(List.of(item));
        return invoiceRepository.save(invoice);
    }

    private BigDecimal requiredMoney(Object value, String fieldName) {
        if (value == null) throw new IllegalArgumentException("Missing " + fieldName + " in payment milestone.");
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrZero(Object value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private Proposal requireOpenProposal(String shareToken) {
        Proposal proposal = requireReadableProposal(shareToken);
        if (proposal.isSigned()) {
            return proposal;
        }
        if (proposal.getExpiresAt() != null && !LocalDateTime.now().isBefore(proposal.getExpiresAt())) {
            throw new IllegalArgumentException("This public proposal has expired.");
        }
        return proposal;
    }

    private Proposal requireReadableProposal(String shareToken) {
        Proposal proposal = proposalRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired public proposal share token."));

        if (proposal.isRevoked()) {
            throw new IllegalArgumentException("This public share link has expired or been revoked.");
        }
        if (!proposal.isSigned() && proposal.getExpiresAt() != null && !LocalDateTime.now().isBefore(proposal.getExpiresAt())) {
            throw new IllegalArgumentException("This public proposal has expired.");
        }
        return proposal;
    }

    private Long agencyIdOf(Proposal proposal) {
        if (proposal.getAgency() == null) {
            throw new IllegalStateException("Proposal is missing an associated Agency, cannot convert safely.");
        }
        return proposal.getAgency().getId();
    }

    private void ensureClientRecord(Proposal proposal, String signerName, String email) {
        Long agencyId = agencyIdOf(proposal);
        Client existing = clientRepository
                .findByAgencyIdAndCompanyNameIgnoreCase(agencyId, proposal.getClientName()).orElse(null);
        if (existing == null) {
            Client client = Client.builder()
                    .agency(proposal.getAgency())
                    .companyName(proposal.getClientName())
                    .contactPerson(signerName)
                    .email(email)
                    .dealStage(DealStage.PROPOSAL_SENT)
                    .build();
            clientRepository.save(client);
        } else {
            // The publishing agency supplied this contact address; acceptance only confirms the same typed address.
            existing.setEmail(email);
            if (existing.getContactPerson() == null || existing.getContactPerson().isBlank()) {
                existing.setContactPerson(signerName);
            }
            clientRepository.save(existing);
        }
    }

    private String computeSignatureHash(Proposal proposal, String signerName, String signerEmail, LocalDateTime signedAt) {
        StringBuilder canonical = new StringBuilder();
        appendCanonicalField(canonical, proposal.getShareToken());
        appendCanonicalField(canonical, proposal.getClientName());
        appendCanonicalField(canonical, proposal.getClientEmail());
        appendCanonicalField(canonical, proposal.getProjectScope());
        appendCanonicalField(canonical, proposal.getDeliverables());
        appendCanonicalField(canonical, proposal.getPaymentTerms());
        appendCanonicalField(canonical, proposal.getDocumentHtml());
        appendCanonicalField(canonical, String.valueOf(proposal.getEstimatedBudget()));
        appendCanonicalField(canonical, signerName);
        appendCanonicalField(canonical, signerEmail);
        appendCanonicalField(canonical, signedAt.toString());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
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

    private void appendCanonicalField(StringBuilder canonical, String value) {
        String normalized = nullToEmpty(value);
        canonical.append(normalized.length()).append(':').append(normalized);
    }

    private PublicPortalProposalDto mapToDto(Proposal proposal) {
        PublicPortalProposalDto dto = PublicPortalProposalDto.builder()
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
        dto.setDocumentHtml(proposal.getDocumentHtml());
        dto.setAgencyName(proposal.getAgency() == null ? "Agency" : proposal.getAgency().getName());
        return dto;
    }
}
