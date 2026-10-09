package com.freelancesuite.repository;

import com.freelancesuite.entity.ContractAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractAuditEventRepository extends JpaRepository<ContractAuditEvent, Long> {

    @Query("SELECT e FROM ContractAuditEvent e WHERE e.contract.id = :contractId ORDER BY e.sequenceNumber ASC")
    List<ContractAuditEvent> findByContractIdOrderBySequence(@Param("contractId") Long contractId);

    @Query("SELECT e FROM ContractAuditEvent e WHERE e.contract.id = :contractId ORDER BY e.sequenceNumber DESC")
    List<ContractAuditEvent> findByContractIdOrderBySequenceDesc(@Param("contractId") Long contractId);

    /**
     * Returns the latest audit event for hash chaining (the prev_event_hash
     * of the next event is this event's event_hash).
     */
    @Query("SELECT e FROM ContractAuditEvent e WHERE e.contract.id = :contractId ORDER BY e.sequenceNumber DESC LIMIT 1")
    Optional<ContractAuditEvent> findLatestByContractId(@Param("contractId") Long contractId);

    @Query("SELECT COALESCE(MAX(e.sequenceNumber), 0) FROM ContractAuditEvent e WHERE e.contract.id = :contractId")
    Long findMaxSequenceNumber(@Param("contractId") Long contractId);
}
