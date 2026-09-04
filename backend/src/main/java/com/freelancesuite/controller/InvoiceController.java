package com.freelancesuite.controller;

import com.freelancesuite.dto.InvoiceDto;
import com.freelancesuite.entity.enums.InvoiceStatus;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceDto>> getAllInvoices(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(invoiceService.getInvoicesForUser(userPrincipal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDto> getInvoiceById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id, userPrincipal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MEMBER')")
    public ResponseEntity<InvoiceDto> createInvoice(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody InvoiceDto dto) {
        return ResponseEntity.ok(invoiceService.createInvoice(dto, userPrincipal.getAgencyId()));
    }

    @PostMapping("/generate-from-time/{projectId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MEMBER')")
    public ResponseEntity<InvoiceDto> generateFromUnbilledTime(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(invoiceService.generateFromUnbilledTime(projectId, userPrincipal.getAgencyId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<InvoiceDto> updateInvoiceStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestParam InvoiceStatus status) {
        return ResponseEntity.ok(invoiceService.updateInvoiceStatus(id, status, userPrincipal.getAgencyId()));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<InputStreamResource> downloadInvoicePdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ByteArrayInputStream pdfStream = invoiceService.generatePdfStream(id, userPrincipal);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=invoice-" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}
