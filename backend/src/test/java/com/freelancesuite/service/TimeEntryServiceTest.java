package com.freelancesuite.service;

import com.freelancesuite.dto.TimeEntryDto;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.Task;
import com.freelancesuite.entity.TimeEntry;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.repository.TaskRepository;
import com.freelancesuite.repository.TimeEntryRepository;
import com.freelancesuite.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimeEntryServiceTest {

    private TimeEntryRepository timeEntryRepository;
    private TaskRepository taskRepository;
    private AppUserRepository userRepository;
    private TimeEntryService service;
    private UserPrincipal principal;
    private Task task;
    private TimeEntry entry;

    @BeforeEach
    void setUp() {
        timeEntryRepository = mock(TimeEntryRepository.class);
        taskRepository = mock(TaskRepository.class);
        userRepository = mock(AppUserRepository.class);
        service = new TimeEntryService(timeEntryRepository, taskRepository, userRepository);
        principal = new UserPrincipal(3L, "Admin", "admin@example.test", "",
                7L, Role.ADMIN, List.of());
        Agency agency = Agency.builder().id(7L).build();
        Client client = Client.builder().agency(agency).build();
        Project project = Project.builder().client(client).build();
        task = Task.builder().id(42L).title("Test task").project(project).actualHours(2.0).build();
        AppUser user = AppUser.builder().id(3L).name("Admin").build();
        entry = TimeEntry.builder().id(11L).task(task).user(user)
                .startTime(LocalDateTime.now().minusMinutes(90)).isBilled(false).build();
    }

    @Test
    void stopTimerRejectsAnAlreadyStoppedEntryWithoutChangingHours() {
        LocalDateTime endTime = LocalDateTime.now().minusMinutes(30);
        entry.setEndTime(endTime);
        entry.setDurationMinutes(60);
        when(timeEntryRepository.findById(11L)).thenReturn(Optional.of(entry));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.stopTimer(11L, principal));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Timer already stopped", error.getReason());
        assertEquals(endTime, entry.getEndTime());
        assertEquals(60, entry.getDurationMinutes());
        assertEquals(2.0, task.getActualHours());
        verify(timeEntryRepository, never()).save(any());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void stopTimerRejectsABilledEntryWithoutChangingHours() {
        entry.setIsBilled(true);
        when(timeEntryRepository.findById(11L)).thenReturn(Optional.of(entry));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.stopTimer(11L, principal));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Time entry already billed", error.getReason());
        assertNull(entry.getEndTime());
        assertEquals(0, entry.getDurationMinutes());
        assertEquals(2.0, task.getActualHours());
        verify(timeEntryRepository, never()).save(any());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void stopTimerAddsElapsedHoursOnceAndRejectsReplay() {
        when(timeEntryRepository.findById(11L)).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(entry)).thenReturn(entry);

        TimeEntryDto result = service.stopTimer(11L, principal);

        assertNotNull(result.getEndTime());
        long elapsedMinutes = Duration.between(entry.getStartTime(), result.getEndTime()).toMinutes();
        assertEquals(elapsedMinutes, result.getDurationMinutes().longValue());
        double expectedHours = 2.0 + elapsedMinutes / 60.0;
        assertEquals(expectedHours, task.getActualHours(), 0.000001);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.stopTimer(11L, principal));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(expectedHours, task.getActualHours(), 0.000001);
        assertEquals(result.getEndTime(), entry.getEndTime());
        verify(taskRepository, times(1)).save(task);
        verify(timeEntryRepository, times(1)).save(entry);
    }
}
