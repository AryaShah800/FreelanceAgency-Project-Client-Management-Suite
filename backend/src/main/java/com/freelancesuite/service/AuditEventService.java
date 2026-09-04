package com.freelancesuite.service;

import com.freelancesuite.dto.AuditEventDto;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.AuditEvent;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.AuditEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;
    private final AgencyRepository agencyRepository;

    @Autowired
    public AuditEventService(AuditEventRepository auditEventRepository, AgencyRepository agencyRepository) {
        this.auditEventRepository = auditEventRepository;
        this.agencyRepository = agencyRepository;
    }

    public List<AuditEventDto> getAuditFeedForAgency(Long agencyId) {
        return auditEventRepository.findByAgencyIdOrderByTimestampDesc(agencyId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void logEvent(Long agencyId, String actor, String action, String details) {
        try {
            Agency agency = agencyRepository.findById(agencyId).orElse(null);
            if (agency == null) return;

            AuditEvent event = AuditEvent.builder()
                    .agency(agency)
                    .actor(actor)
                    .action(action)
                    .details(details)
                    .build();

            auditEventRepository.save(event);
        } catch (Exception e) {
            System.err.println("Audit logging warning: " + e.getMessage());
        }
    }

    private AuditEventDto mapToDto(AuditEvent event) {
        return AuditEventDto.builder()
                .id(event.getId())
                .agencyId(event.getAgency().getId())
                .actor(event.getActor())
                .action(event.getAction())
                .details(event.getDetails())
                .timestamp(event.getTimestamp())
                .build();
    }
}
