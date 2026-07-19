package com.freelancesuite.repository;

import com.freelancesuite.entity.Task;
import com.freelancesuite.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
    List<Task> findByAssignedToId(Long userId);

    @Query("SELECT t FROM Task t WHERE t.project.client.agency.id = :agencyId")
    List<Task> findByAgencyId(@Param("agencyId") Long agencyId);
}
