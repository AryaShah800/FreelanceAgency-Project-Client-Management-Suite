package com.freelancesuite.repository;

import com.freelancesuite.entity.SignatureEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SignatureEventRepository extends JpaRepository<SignatureEvent, Long> {

    @Query("SELECT e FROM SignatureEvent e WHERE e.contractVersion.id = :versionId ORDER BY e.signedAt ASC")
    List<SignatureEvent> findByContractVersionId(@Param("versionId") Long versionId);

    @Query("SELECT e FROM SignatureEvent e WHERE e.contractSigner.id = :signerId")
    List<SignatureEvent> findByContractSignerId(@Param("signerId") Long signerId);
}
