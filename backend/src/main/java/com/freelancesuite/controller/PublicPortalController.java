package com.freelancesuite.controller;

import com.freelancesuite.dto.PublicPortalProposalDto;
import com.freelancesuite.service.ProposalConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/portal")
public class PublicPortalController {

    private final ProposalConversionService conversionService;

    @Autowired
    public PublicPortalController(ProposalConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicPortalProposalDto> getPublicProposal(@PathVariable String token) {
        return ResponseEntity.ok(conversionService.getPublicProposal(token));
    }

    @PostMapping("/{token}/sign")
    public ResponseEntity<PublicPortalProposalDto> signPublicProposal(
            @PathVariable String token,
            @RequestBody Map<String, String> body) {
        String signerName = body.getOrDefault("signerName", "Valued Client");
        return ResponseEntity.ok(conversionService.signPublicProposal(token, signerName));
    }

    @PostMapping("/{token}/convert")
    public ResponseEntity<Map<String, Object>> convertToProject(@PathVariable String token) {
        return ResponseEntity.ok(conversionService.convertProposalToProject(token));
    }
}
