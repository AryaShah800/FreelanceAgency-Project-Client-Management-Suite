package com.freelancesuite.repository;

import com.freelancesuite.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByTaskId(Long taskId);

    Optional<TimeEntry> findByUserIdAndEndTimeIsNull(Long userId);

    @Query("SELECT te FROM TimeEntry te WHERE te.task.project.id = :projectId AND (te.isBilled = false OR te.isBilled IS NULL) AND te.endTime IS NOT NULL")
    List<TimeEntry> findUnbilledTimeEntriesByProject(@Param("projectId") Long projectId);

    List<TimeEntry> findByTaskProjectId(Long projectId);

    @Query("SELECT te.task.project.id, SUM(te.durationMinutes / 60.0 * COALESCE(te.user.hourlyRate, 0.0)) " +
           "FROM TimeEntry te WHERE te.task.project.client.agency.id = :agencyId " +
           "AND te.task.project.id IN :projectIds AND te.endTime IS NOT NULL AND te.durationMinutes > 0 " +
           "GROUP BY te.task.project.id")
    List<Object[]> sumLoggedValueByProjectIds(@Param("agencyId") Long agencyId,
                                            @Param("projectIds") Collection<Long> projectIds);

    long countByTaskProjectId(Long projectId);
    long countByTaskId(Long taskId);
}
