package com.freelancesuite.controller;

import com.freelancesuite.dto.PublicPortalProposalDto;
import com.freelancesuite.dto.ProposalAcceptanceRequest;
import jakarta.validation.Valid;
import com.freelancesuite.service.ProposalConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/public/portal")
public class PublicPortalController {

    private final ProposalConversionService conversionService;
    private final com.github.benmanes.caffeine.cache.Cache<String, Integer> requestCounts =
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .expireAfterWrite(1, java.util.concurrent.TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private void enforceRateLimit(String ip) {
        Integer count = requestCounts.asMap().compute(ip, (k, v) -> (v == null) ? 1 : v + 1);
        if (count > 20) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }

    @Autowired
    public PublicPortalController(ProposalConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicPortalProposalDto> getPublicProposal(
            @PathVariable String token,
            jakarta.servlet.http.HttpServletRequest request) {
        enforceRateLimit(request.getRemoteAddr());
        return ResponseEntity.ok(conversionService.getPublicProposal(token));
    }

    @PostMapping("/{token}/acceptance")
    public ResponseEntity<PublicPortalProposalDto> recordAcceptance(
            @PathVariable String token,
            @Valid @RequestBody ProposalAcceptanceRequest body,
            jakarta.servlet.http.HttpServletRequest request) {
        enforceRateLimit(request.getRemoteAddr());
        return ResponseEntity.ok(conversionService.recordProposalAcceptance(
                token, body.getSignerName(), body.getSignerEmail()));
    }

    @PostMapping("/{token}/convert")
    public ResponseEntity<Map<String, Object>> convertToProject(
            @PathVariable String token,
            jakarta.servlet.http.HttpServletRequest request) {
        enforceRateLimit(request.getRemoteAddr());
        return ResponseEntity.ok(conversionService.convertProposalToProject(token));
    }

    @GetMapping("/{token}/acceptance-record.pdf")
    public ResponseEntity<byte[]> downloadAcceptanceRecord(
            @PathVariable String token,
            jakarta.servlet.http.HttpServletRequest request) {
        enforceRateLimit(request.getRemoteAddr());
        byte[] pdf = conversionService.getAcceptanceRecordPdf(token);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=proposal-acceptance-record.pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
