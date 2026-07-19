package com.freelancesuite.repository;

import com.freelancesuite.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Boolean existsByEmail(String email);
    List<AppUser> findByAgencyId(Long agencyId);
}
