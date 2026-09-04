package com.freelancesuite.service;

import com.freelancesuite.dto.InvoiceDto;
import com.freelancesuite.dto.InvoiceLineItemDto;
import com.freelancesuite.dto.PublicPortalProposalDto;
import com.freelancesuite.entity.*;
import com.freelancesuite.entity.enums.DealStage;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProposalConversionService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditEventService auditEventService;

    // In-memory token registry for share tokens
    private final Map<String, PublicPortalProposalDto> tokenRegistry = new ConcurrentHashMap<>();

    @Autowired
    public ProposalConversionService(ProjectRepository projectRepository, ClientRepository clientRepository, InvoiceRepository invoiceRepository, AuditEventService auditEventService) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditEventService = auditEventService;

        // Seed demo share token for instant testing
        seedDemoShareToken();
    }

    private void seedDemoShareToken() {
        String demoToken = "demo-proposal-token-2026";
        PublicPortalProposalDto dto = PublicPortalProposalDto.builder()
                .shareToken(demoToken)
                .clientName("FinTech Innovations")
                .projectScope("Spring Boot Banking REST APIs, OpenPDF GST Invoices & React Dashboard")
                .deliverables("- Milestone 1: Core Spring Boot Security & JWT\n- Milestone 2: STOMP WebSockets & Live Kanban\n- Milestone 3: Razorpay Payment Gateway & OpenPDF Streamer")
                .paymentTerms("50% advance deposit upon signing, 50% upon final acceptance")
                .estimatedBudget(new BigDecimal("450000.00"))
                .isSigned(false)
                .isConvertedToProject(false)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isRevoked(false)
                .build();

        tokenRegistry.put(demoToken, dto);
    }

    public PublicPortalProposalDto getPublicProposal(String shareToken) {
        PublicPortalProposalDto dto = tokenRegistry.get(shareToken);
        if (dto == null) {
            throw new IllegalArgumentException("Invalid or expired public proposal share token.");
        }

        if (Boolean.TRUE.equals(dto.getIsRevoked()) || (dto.getExpiresAt() != null && LocalDateTime.now().isAfter(dto.getExpiresAt()))) {
            throw new IllegalArgumentException("This public share link has expired or been revoked.");
        }

        // Log audit event asynchronously
        auditEventService.logEvent(1L, "Public Client", "PROPOSAL_VIEWED", "Client viewed public proposal via token: " + shareToken);

        return dto;
    }

    @Transactional
    public PublicPortalProposalDto signPublicProposal(String shareToken, String signerName) {
        PublicPortalProposalDto dto = getPublicProposal(shareToken);

        dto.setIsSigned(true);
        dto.setSignatureName(signerName);
        dto.setSignedAt(LocalDateTime.now());

        tokenRegistry.put(shareToken, dto);

        auditEventService.logEvent(1L, signerName + " (Client)", "CONTRACT_SIGNED", "Proposal digitally signed via public portal token: " + shareToken);

        return dto;
    }

    @Transactional
    public Map<String, Object> convertProposalToProject(String shareToken) {
        PublicPortalProposalDto dto = getPublicProposal(shareToken);

        if (Boolean.TRUE.equals(dto.getIsConvertedToProject())) {
            throw new IllegalStateException("Proposal has already been converted to an active project.");
        }

        // 1. Fetch or create client
        Client client = clientRepository.findAll().stream()
                .filter(c -> c.getCompanyName().equalsIgnoreCase(dto.getClientName()))
                .findFirst()
                .orElseGet(() -> {
                    Client newC = Client.builder()
                            .companyName(dto.getClientName())
                            .contactPerson("Sarah Jenkins")
                            .email("sarah@fintech.io")
                            .dealStage(DealStage.WON)
                            .build();
                    return clientRepository.save(newC);
                });

        client.setDealStage(DealStage.WON);
        clientRepository.save(client);

        // 2. Create Project Entity
        Project project = Project.builder()
                .client(client)
                .title(dto.getClientName() + " - " + dto.getProjectScope().substring(0, Math.min(30, dto.getProjectScope().length())))
                .description(dto.getProjectScope())
                .budget(dto.getEstimatedBudget())
                .deadline(LocalDate.now().plusMonths(2))
                .build();

        project = projectRepository.save(project);

        // 3. Auto-generate 50% Deposit Invoice (INV-DEP-...)
        BigDecimal totalBudget = dto.getEstimatedBudget();
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

        // 4. Update Proposal Token DTO State
        dto.setIsConvertedToProject(true);
        dto.setCreatedProjectId(project.getId());
        dto.setDepositInvoiceNumber(depositInvoice.getInvoiceNumber());
        dto.setDepositAmount(totalAmount);

        tokenRegistry.put(shareToken, dto);

        // 5. Log Audit Event
        Long agencyId = client.getAgency() != null ? client.getAgency().getId() : 1L;
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
}
