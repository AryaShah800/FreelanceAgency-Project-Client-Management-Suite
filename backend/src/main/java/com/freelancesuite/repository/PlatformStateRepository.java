package com.freelancesuite.repository;

import com.freelancesuite.entity.PlatformState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PlatformStateRepository extends JpaRepository<PlatformState, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from PlatformState state where state.id = 1")
    PlatformState lockBootstrapState();
}
