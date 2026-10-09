package com.freelancesuite.repository;

import com.freelancesuite.entity.AgencyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgencyInvitationRepository extends JpaRepository<AgencyInvitation, Long> {
    Optional<AgencyInvitation> findByTokenHash(String tokenHash);
    Optional<AgencyInvitation> findFirstByAgencyIdAndEmailAndAcceptedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
            Long agencyId, String email);
    List<AgencyInvitation> findByAgencyIdAndAcceptedAtIsNullAndRevokedAtIsNull(Long agencyId);
}
