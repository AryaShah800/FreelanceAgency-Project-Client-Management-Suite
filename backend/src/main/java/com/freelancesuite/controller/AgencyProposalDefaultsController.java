package com.freelancesuite.controller;

import com.freelancesuite.dto.ProposalDefaultsDto;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/agency/proposal-defaults")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class AgencyProposalDefaultsController {
    private final AgencyRepository agencies;
    public AgencyProposalDefaultsController(AgencyRepository agencies) { this.agencies = agencies; }

    @GetMapping
    public ResponseEntity<ProposalDefaultsDto> getDefaults(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(map(findAgency(user.getAgencyId())));
    }

    @PatchMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ProposalDefaultsDto> saveDefaults(@AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ProposalDefaultsDto defaults) {
        Agency agency = findAgency(user.getAgencyId());
        agency.setTaxState(clean(defaults.getState()));
        agency.setGstRegistered(defaults.getIsGstRegistered());
        agency.setLutDetails(clean(defaults.getLutDetails()));
        agency.setLegalTemplate(clean(defaults.getLegalTemplate()));
        agency.setProposalDefaultsInitialized(true);
        return ResponseEntity.ok(map(agencies.save(agency)));
    }

    private Agency findAgency(Long agencyId) {
        return agencies.findById(agencyId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency profile was not found"));
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private ProposalDefaultsDto map(Agency agency) {
        ProposalDefaultsDto dto = new ProposalDefaultsDto();
        dto.setState(agency.getTaxState()); dto.setIsGstRegistered(agency.isGstRegistered());
        dto.setLutDetails(agency.getLutDetails()); dto.setLegalTemplate(agency.getLegalTemplate());
        dto.setInitialized(agency.isProposalDefaultsInitialized());
        return dto;
    }
}
