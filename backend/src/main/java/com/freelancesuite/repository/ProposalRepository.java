package com.freelancesuite.repository;

import com.freelancesuite.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    Optional<Proposal> findByShareToken(String shareToken);
    Optional<Proposal> findByIdAndAgencyId(Long id, Long agencyId);
    List<Proposal> findByAgencyIdOrderByCreatedAtDesc(Long agencyId);
}
