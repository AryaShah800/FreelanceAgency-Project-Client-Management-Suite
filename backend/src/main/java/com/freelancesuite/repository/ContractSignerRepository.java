package com.freelancesuite.repository;

import com.freelancesuite.entity.ContractSigner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractSignerRepository extends JpaRepository<ContractSigner, Long> {

    @Query("SELECT s FROM ContractSigner s WHERE s.contractVersion.id = :versionId ORDER BY s.signingOrder ASC")
    List<ContractSigner> findByContractVersionIdOrderBySigningOrder(@Param("versionId") Long versionId);
}
