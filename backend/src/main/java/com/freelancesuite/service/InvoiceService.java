package com.freelancesuite.service;

import com.freelancesuite.dto.InvoiceDto;
import com.freelancesuite.dto.InvoiceLineItemDto;
import com.freelancesuite.entity.Invoice;
import com.freelancesuite.entity.InvoiceLineItem;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.util.PdfGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final PdfGeneratorUtil pdfGeneratorUtil;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository, ProjectRepository projectRepository, PdfGeneratorUtil pdfGeneratorUtil) {
        this.invoiceRepository = invoiceRepository;
        this.projectRepository = projectRepository;
        this.pdfGeneratorUtil = pdfGeneratorUtil;
    }

    public List<InvoiceDto> getInvoicesForUser(UserPrincipal userPrincipal) {
        List<Invoice> invoices;
        boolean isClient = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient) {
            // Client Portal: Only show invoices belonging to this client's email
            invoices = invoiceRepository.findByAgencyId(userPrincipal.getAgencyId())
                    .stream()
                    .filter(i -> i.getProject().getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail()))
                    .collect(Collectors.toList());
        } else {
            // Owner / Member: Show all agency invoices
            invoices = invoiceRepository.findByAgencyId(userPrincipal.getAgencyId());
        }

        return invoices.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public InvoiceDto getInvoiceById(Long id, UserPrincipal userPrincipal) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (!invoice.getProject().getClient().getAgency().getId().equals(userPrincipal.getAgencyId())) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        boolean isClient = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient && !invoice.getProject().getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail())) {
            throw new IllegalArgumentException("Unauthorized invoice access");
        }

        return mapToDto(invoice);
    }

    @Transactional
    public InvoiceDto createInvoice(InvoiceDto dto, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(dto.getProjectId(), agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<InvoiceLineItem> lineItems = new ArrayList<>();

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

        BigDecimal gstRate = new BigDecimal("0.09");
        BigDecimal cgst = subtotal.multiply(gstRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = subtotal.multiply(gstRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(cgst).add(sgst);

        String invNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Invoice invoice = Invoice.builder()
                .project(project)
                .invoiceNumber(invNumber)
                .subtotal(subtotal)
                .cgst(cgst)
                .sgst(sgst)
                .totalAmount(totalAmount)
                .status(InvoiceStatus.DRAFT)
                .dueDate(dto.getDueDate() != null ? dto.getDueDate() : LocalDate.now().plusDays(15))
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .build();

        for (InvoiceLineItem lineItem : lineItems) {
            lineItem.setInvoice(invoice);
        }
        invoice.setLineItems(lineItems);

        return mapToDto(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceDto updateInvoiceStatus(Long id, InvoiceStatus status, Long agencyId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (!invoice.getProject().getClient().getAgency().getId().equals(agencyId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        invoice.setStatus(status);
        return mapToDto(invoiceRepository.save(invoice));
    }

    public ByteArrayInputStream generatePdfStream(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        return pdfGeneratorUtil.generateInvoicePdf(invoice);
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processRecurringInvoices() {
        List<Invoice> recurringInvoices = invoiceRepository.findByIsRecurringTrue();
        for (Invoice inv : recurringInvoices) {
            if (inv.getDueDate().isBefore(LocalDate.now())) {
                Invoice newInv = Invoice.builder()
                        .project(inv.getProject())
                        .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .subtotal(inv.getSubtotal())
                        .cgst(inv.getCgst())
                        .sgst(inv.getSgst())
                        .totalAmount(inv.getTotalAmount())
                        .status(InvoiceStatus.SENT)
                        .dueDate(LocalDate.now().plusMonths(1))
                        .isRecurring(true)
                        .build();

                invoiceRepository.save(newInv);
            }
        }
    }

    private InvoiceDto mapToDto(Invoice invoice) {
        List<InvoiceLineItemDto> items = invoice.getLineItems().stream().map(i -> InvoiceLineItemDto.builder()
                .id(i.getId())
                .description(i.getDescription())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .amount(i.getAmount())
                .build()).collect(Collectors.toList());

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
