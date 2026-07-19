package com.freelancesuite.repository;

import com.freelancesuite.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p WHERE p.client.agency.id = :agencyId")
    List<Project> findByAgencyId(@Param("agencyId") Long agencyId);

    List<Project> findByClientId(Long clientId);

    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.client.agency.id = :agencyId")
    Optional<Project> findByIdAndAgencyId(@Param("id") Long id, @Param("agencyId") Long agencyId);
}
