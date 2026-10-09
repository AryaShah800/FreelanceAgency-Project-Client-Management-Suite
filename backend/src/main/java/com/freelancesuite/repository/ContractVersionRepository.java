package com.freelancesuite.repository;

import com.freelancesuite.entity.ContractVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {

    @Query("SELECT v FROM ContractVersion v WHERE v.contract.id = :contractId ORDER BY v.versionNumber DESC")
    List<ContractVersion> findByContractIdOrderByVersionDesc(@Param("contractId") Long contractId);

    @Query("SELECT v FROM ContractVersion v WHERE v.contract.id = :contractId AND v.versionNumber = :versionNumber")
    Optional<ContractVersion> findByContractIdAndVersionNumber(
            @Param("contractId") Long contractId,
            @Param("versionNumber") Integer versionNumber);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM ContractVersion v WHERE v.contract.id = :contractId")
    Integer findMaxVersionNumber(@Param("contractId") Long contractId);
}
