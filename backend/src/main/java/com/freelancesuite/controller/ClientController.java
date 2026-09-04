package com.freelancesuite.controller;

import com.freelancesuite.dto.ClientDto;
import com.freelancesuite.entity.enums.DealStage;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@PreAuthorize("hasAnyRole('OWNER', 'MEMBER')")
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<List<ClientDto>> getAllClients(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) DealStage stage) {
        return ResponseEntity.ok(clientService.getAllClients(userPrincipal.getAgencyId(), stage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getClientById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id, userPrincipal.getAgencyId()));
    }

    @PostMapping
    public ResponseEntity<ClientDto> createClient(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ClientDto dto) {
        return ResponseEntity.ok(clientService.createClient(dto, userPrincipal.getAgencyId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> updateClient(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody ClientDto dto) {
        return ResponseEntity.ok(clientService.updateClient(id, dto, userPrincipal.getAgencyId()));
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<ClientDto> updateDealStage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestParam DealStage stage) {
        return ResponseEntity.ok(clientService.updateDealStage(id, stage, userPrincipal.getAgencyId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        clientService.deleteClient(id, userPrincipal.getAgencyId());
        return ResponseEntity.noContent().build();
    }
}
