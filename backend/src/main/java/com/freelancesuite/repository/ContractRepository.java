package com.freelancesuite.repository;

import com.freelancesuite.entity.Contract;
import com.freelancesuite.entity.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("SELECT c FROM Contract c WHERE c.agency.id = :agencyId ORDER BY c.createdAt DESC")
    List<Contract> findByAgencyId(@Param("agencyId") Long agencyId);

    Optional<Contract> findByShareToken(String shareToken);

    Optional<Contract> findByContractNumber(String contractNumber);

    @Query("SELECT c FROM Contract c WHERE c.agency.id = :agencyId AND c.status = :status")
    List<Contract> findByAgencyIdAndStatus(@Param("agencyId") Long agencyId, @Param("status") ContractStatus status);

    @Query("SELECT c FROM Contract c WHERE c.proposal.id = :proposalId")
    Optional<Contract> findByProposalId(@Param("proposalId") Long proposalId);
}
