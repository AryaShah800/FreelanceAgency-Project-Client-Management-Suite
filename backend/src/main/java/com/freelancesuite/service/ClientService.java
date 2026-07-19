package com.freelancesuite.service;

import com.freelancesuite.dto.ClientDto;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.enums.DealStage;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AgencyRepository agencyRepository;

    @Autowired
    public ClientService(ClientRepository clientRepository, AgencyRepository agencyRepository) {
        this.clientRepository = clientRepository;
        this.agencyRepository = agencyRepository;
    }

    public List<ClientDto> getAllClients(Long agencyId, DealStage dealStage) {
        List<Client> clients;
        if (dealStage != null) {
            clients = clientRepository.findByAgencyIdAndDealStage(agencyId, dealStage);
        } else {
            clients = clientRepository.findByAgencyId(agencyId);
        }

        return clients.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ClientDto getClientById(Long id, Long agencyId) {
        Client client = clientRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        return mapToDto(client);
    }

    @Transactional
    public ClientDto createClient(ClientDto dto, Long agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Agency not found"));

        Client client = Client.builder()
                .agency(agency)
                .companyName(dto.getCompanyName())
                .contactPerson(dto.getContactPerson())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .gstin(dto.getGstin())
                .dealStage(dto.getDealStage() != null ? dto.getDealStage() : DealStage.LEAD)
                .build();

        client = clientRepository.save(client);
        return mapToDto(client);
    }

    @Transactional
    public ClientDto updateClient(Long id, ClientDto dto, Long agencyId) {
        Client client = clientRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        client.setCompanyName(dto.getCompanyName());
        client.setContactPerson(dto.getContactPerson());
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
        client.setGstin(dto.getGstin());
        if (dto.getDealStage() != null) {
            client.setDealStage(dto.getDealStage());
        }

        return mapToDto(clientRepository.save(client));
    }

    @Transactional
    public ClientDto updateDealStage(Long id, DealStage dealStage, Long agencyId) {
        Client client = clientRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        client.setDealStage(dealStage);
        return mapToDto(clientRepository.save(client));
    }

    @Transactional
    public void deleteClient(Long id, Long agencyId) {
        Client client = clientRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        clientRepository.delete(client);
    }

    private ClientDto mapToDto(Client client) {
        return ClientDto.builder()
                .id(client.getId())
                .companyName(client.getCompanyName())
                .contactPerson(client.getContactPerson())
                .email(client.getEmail())
                .phone(client.getPhone())
                .gstin(client.getGstin())
                .dealStage(client.getDealStage())
                .createdAt(client.getCreatedAt())
                .build();
    }
}
