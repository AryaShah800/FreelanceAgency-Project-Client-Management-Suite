package com.freelancesuite.repository;

import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.enums.DealStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByAgencyId(Long agencyId);
    List<Client> findByAgencyIdAndDealStage(Long agencyId, DealStage dealStage);
    Optional<Client> findByIdAndAgencyId(Long id, Long agencyId);
}
