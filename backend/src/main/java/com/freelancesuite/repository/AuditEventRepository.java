package com.freelancesuite.repository;

import com.freelancesuite.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByAgencyIdOrderByTimestampDesc(Long agencyId);
}
