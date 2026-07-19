package com.freelancesuite.repository;

import com.freelancesuite.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    List<TimeEntry> findByTaskId(Long taskId);
    List<TimeEntry> findByUserId(Long userId);
    Optional<TimeEntry> findByUserIdAndEndTimeIsNull(Long userId);
}
