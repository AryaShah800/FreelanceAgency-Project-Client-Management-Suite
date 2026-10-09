package com.freelancesuite.controller;

import com.freelancesuite.dto.AgencyProfileDto;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/agency/profile")
@PreAuthorize("hasRole('OWNER')")
public class AgencyProfileController {
    private final AgencyRepository agencyRepository;

    public AgencyProfileController(AgencyRepository agencyRepository) {
        this.agencyRepository = agencyRepository;
    }

    @GetMapping
    public ResponseEntity<AgencyProfileDto> getProfile(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(toDto(findAgency(user.getAgencyId())));
    }

    @PutMapping
    public ResponseEntity<AgencyProfileDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody AgencyProfileDto profile) {
        Agency agency = findAgency(user.getAgencyId());
        agency.setName(clean(profile.getName()));
        agency.setGstin(clean(profile.getGstin()).toUpperCase(java.util.Locale.ROOT));
        agency.setPan(clean(profile.getPan()).toUpperCase(java.util.Locale.ROOT));
        agency.setRegisteredAddress(clean(profile.getRegisteredAddress()));
        agency.setStateCode(clean(profile.getStateCode()));
        agency.setBankName(clean(profile.getBankName()));
        agency.setBankAccountNumber(clean(profile.getBankAccountNumber()));
        agency.setBankIfsc(clean(profile.getBankIfsc()).toUpperCase(java.util.Locale.ROOT));
        return ResponseEntity.ok(toDto(agencyRepository.save(agency)));
    }

    private Agency findAgency(Long agencyId) {
        return agencyRepository.findById(agencyId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency profile was not found"));
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private AgencyProfileDto toDto(Agency agency) {
        return new AgencyProfileDto(agency.getName(), agency.getGstin(), agency.getPan(),
                agency.getRegisteredAddress(), agency.getStateCode(), agency.getBankName(),
                agency.getBankAccountNumber(), agency.getBankIfsc());
    }
}
