package com.freelancesuite.service;

import com.freelancesuite.dto.InvoiceDto;
import com.freelancesuite.dto.InvoiceLineItemDto;
import com.freelancesuite.entity.*;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.repository.*;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.util.PdfGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final TimeEntryRepository timeEntryRepository;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository, ProjectRepository projectRepository, TimeEntryRepository timeEntryRepository) {
        this.invoiceRepository = invoiceRepository;
        this.projectRepository = projectRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    public List<InvoiceDto> getInvoicesForUser(UserPrincipal userPrincipal) {
        List<Invoice> invoices = invoiceRepository.findByAgencyId(userPrincipal.getAgencyId());

        boolean isClient = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient) {
            invoices = invoices.stream()
                    .filter(inv -> inv.getProject().getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail()))
                    .collect(Collectors.toList());
        }

        return invoices.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public InvoiceDto getInvoiceById(Long id, UserPrincipal userPrincipal) {
        return mapToDto(requireAccessibleInvoice(id, userPrincipal));
    }

    @Transactional
    public InvoiceDto createInvoice(InvoiceDto dto, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(dto.getProjectId(), agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        List<InvoiceLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        if (dto.getLineItems() != null) {
            for (InvoiceLineItemDto itemDto : dto.getLineItems()) {
                BigDecimal itemAmount = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                subtotal = subtotal.add(itemAmount);

                InvoiceLineItem item = InvoiceLineItem.builder()
                        .description(itemDto.getDescription())
                        .quantity(itemDto.getQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .amount(itemAmount)
                        .build();
                lineItems.add(item);
            }
        }

        // Multi-State GST Tax Engine logic
        TaxBreakdown tax = calculateMultiStateGst(subtotal, project);

        Invoice invoice = Invoice.builder()
                .project(project)
                .invoiceNumber("INV-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                .subtotal(subtotal)
                .cgst(tax.cgst)
                .sgst(tax.sgst)
                .igst(tax.igst)
                .totalAmount(tax.totalAmount)
                .status(InvoiceStatus.DRAFT)
                .dueDate(dto.getDueDate() != null ? dto.getDueDate() : LocalDate.now().plusDays(14))
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .build();

        for (InvoiceLineItem item : lineItems) {
            item.setInvoice(invoice);
        }
        invoice.setLineItems(lineItems);

        invoice = invoiceRepository.save(invoice);
        return mapToDto(invoice);
    }

    @Transactional
    public InvoiceDto generateFromUnbilledTime(Long projectId, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(projectId, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        List<TimeEntry> unbilled = timeEntryRepository.findUnbilledTimeEntriesByProject(projectId);
        if (unbilled.isEmpty()) {
            throw new IllegalArgumentException("No unbilled time entries found for this project.");
        }

        List<InvoiceLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (TimeEntry te : unbilled) {
            double hours = te.getDurationMinutes() / 60.0;
            double rate = (te.getUser() != null && te.getUser().getHourlyRate() != null && te.getUser().getHourlyRate() > 0)
                    ? te.getUser().getHourlyRate() : 150.0;

            BigDecimal amount = BigDecimal.valueOf(hours * rate).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(amount);

            InvoiceLineItem item = InvoiceLineItem.builder()
                    .description("Logged Work: " + te.getTask().getTitle() + " (" + String.format("%.2f", hours) + " hrs @ ₹" + rate + "/hr)")
                    .quantity(1)
                    .unitPrice(amount)
                    .amount(amount)
                    .build();
            lineItems.add(item);
        }

        // Multi-State GST Tax Engine logic
        TaxBreakdown tax = calculateMultiStateGst(subtotal, project);

        Invoice invoice = Invoice.builder()
                .project(project)
                .invoiceNumber("INV-TIME-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .subtotal(subtotal)
                .cgst(tax.cgst)
                .sgst(tax.sgst)
                .igst(tax.igst)
                .totalAmount(tax.totalAmount)
                .status(InvoiceStatus.DRAFT)
                .dueDate(LocalDate.now().plusDays(14))
                .isRecurring(false)
                .build();

        for (InvoiceLineItem item : lineItems) {
            item.setInvoice(invoice);
        }
        invoice.setLineItems(lineItems);
        invoice = invoiceRepository.save(invoice);

        // Mark time entries as billed
        for (TimeEntry te : unbilled) {
            te.setIsBilled(true);
            te.setInvoice(invoice);
            timeEntryRepository.save(te);
        }

        return mapToDto(invoice);
    }

    @Transactional
    public InvoiceDto updateInvoiceStatus(Long id, InvoiceStatus status, Long agencyId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (!invoice.getProject().getClient().getAgency().getId().equals(agencyId)) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot update this invoice");
        }

        invoice.setStatus(status);
        invoice = invoiceRepository.save(invoice);
        return mapToDto(invoice);
    }

    public ByteArrayInputStream generatePdfStream(Long id, UserPrincipal userPrincipal) {
        return PdfGeneratorUtil.generateInvoicePdf(requireAccessibleInvoice(id, userPrincipal));
    }

    private Invoice requireAccessibleInvoice(Long id, UserPrincipal userPrincipal) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (!invoice.getProject().getClient().getAgency().getId().equals(userPrincipal.getAgencyId())) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access this invoice");
        }
        boolean isClient = userPrincipal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
        if (isClient && !invoice.getProject().getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access this invoice");
        }
        return invoice;
    }

    private TaxBreakdown calculateMultiStateGst(BigDecimal subtotal, Project project) {
        String agencyGstin = project.getClient().getAgency().getGstin();
        String clientGstin = project.getClient().getGstin();

        String agencyState = (agencyGstin != null && agencyGstin.length() >= 2) ? agencyGstin.substring(0, 2) : "27";
        String clientState = (clientGstin != null && clientGstin.length() >= 2) ? clientGstin.substring(0, 2) : "27";

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        if (agencyState.equalsIgnoreCase(clientState)) {
            // Intra-state (CGST 9% + SGST 9%)
            cgst = subtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
            sgst = subtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Inter-state (IGST 18%)
            igst = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = subtotal.add(cgst).add(sgst).add(igst);
        return new TaxBreakdown(cgst, sgst, igst, total);
    }

    private static class TaxBreakdown {
        final BigDecimal cgst;
        final BigDecimal sgst;
        final BigDecimal igst;
        final BigDecimal totalAmount;

        TaxBreakdown(BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal totalAmount) {
            this.cgst = cgst;
            this.sgst = sgst;
            this.igst = igst;
            this.totalAmount = totalAmount;
        }
    }

    private InvoiceDto mapToDto(Invoice invoice) {
        List<InvoiceLineItemDto> items = invoice.getLineItems().stream()
                .map(item -> InvoiceLineItemDto.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .amount(item.getAmount())
                        .build())
                .collect(Collectors.toList());

        return InvoiceDto.builder()
                .id(invoice.getId())
                .projectId(invoice.getProject().getId())
                .projectTitle(invoice.getProject().getTitle())
                .clientName(invoice.getProject().getClient().getCompanyName())
                .invoiceNumber(invoice.getInvoiceNumber())
                .subtotal(invoice.getSubtotal())
                .cgst(invoice.getCgst())
                .sgst(invoice.getSgst())
                .igst(invoice.getIgst())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .dueDate(invoice.getDueDate())
                .isRecurring(invoice.getIsRecurring())
                .lineItems(items)
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
