package com.freelancesuite.service;

import com.freelancesuite.dto.TimeEntryDto;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.Task;
import com.freelancesuite.entity.TimeEntry;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.repository.TaskRepository;
import com.freelancesuite.repository.TimeEntryRepository;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;

    @Autowired
    public TimeEntryService(TimeEntryRepository timeEntryRepository, TaskRepository taskRepository, AppUserRepository userRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public Optional<TimeEntryDto> getActiveTimer(UserPrincipal userPrincipal) {
        return timeEntryRepository.findByUserIdAndEndTimeIsNull(userPrincipal.getId())
                .map(this::mapToDto);
    }

    @Transactional
    public TimeEntryDto startTimer(Long taskId, UserPrincipal userPrincipal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (!task.getProject().getClient().getAgency().getId().equals(userPrincipal.getAgencyId())) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot track time for this task");
        }

        Optional<TimeEntry> runningTimer = timeEntryRepository.findByUserIdAndEndTimeIsNull(userPrincipal.getId());
        if (runningTimer.isPresent()) {
            stopTimer(runningTimer.get().getId(), userPrincipal);
        }

        AppUser user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TimeEntry entry = TimeEntry.builder()
                .task(task)
                .user(user)
                .startTime(LocalDateTime.now())
                .isBilled(false)
                .build();

        entry = timeEntryRepository.save(entry);
        return mapToDto(entry);
    }

    @Transactional
    public TimeEntryDto stopTimer(Long entryId, UserPrincipal userPrincipal) {
        TimeEntry entry = timeEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Time entry not found"));

        if (!entry.getUser().getId().equals(userPrincipal.getId())) {
            throw new IllegalArgumentException("Unauthorized time entry modification");
        }

        LocalDateTime now = LocalDateTime.now();
        entry.setEndTime(now);
        long minutes = Duration.between(entry.getStartTime(), now).toMinutes();
        int finalMinutes = (int) Math.max(1, minutes);
        entry.setDurationMinutes(finalMinutes);

        Task task = entry.getTask();
        double currentActual = task.getActualHours() != null ? task.getActualHours() : 0.0;
        task.setActualHours(currentActual + (finalMinutes / 60.0));
        taskRepository.save(task);

        entry = timeEntryRepository.save(entry);
        return mapToDto(entry);
    }

    private TimeEntryDto mapToDto(TimeEntry entry) {
        return TimeEntryDto.builder()
                .id(entry.getId())
                .taskId(entry.getTask().getId())
                .taskTitle(entry.getTask().getTitle())
                .userId(entry.getUser().getId())
                .userName(entry.getUser().getName())
                .startTime(entry.getStartTime())
                .endTime(entry.getEndTime())
                .durationMinutes(entry.getDurationMinutes())
                .isBilled(entry.getIsBilled())
                .build();
    }
}
