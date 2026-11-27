package com.freelancesuite.service;

import com.freelancesuite.dto.ProjectDto;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.Task;
import com.freelancesuite.entity.TimeEntry;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.repository.ClientRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.repository.TaskRepository;
import com.freelancesuite.repository.TimeEntryRepository;
import com.freelancesuite.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectLoggedValueIntegrationTest {

    @Autowired private ProjectService projectService;
    @Autowired private AgencyRepository agencyRepo;
    @Autowired private AppUserRepository userRepo;
    @Autowired private ClientRepository clientRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TaskRepository taskRepo;
    @MockitoSpyBean private TimeEntryRepository timeEntryRepo;

    private Agency agency;
    private AppUser owner;
    private Client client;
    private Project project, emptyProject, otherProject;

    @BeforeEach
    void setup() {
        agency = agencyRepo.save(Agency.builder().name("Logged value agency").build());
        Agency otherAgency = agencyRepo.save(Agency.builder().name("Other agency").build());
        owner = user(agency, 120.0);
        AppUser secondUser = user(agency, 100.5);
        AppUser noRateUser = user(agency, null);
        client = clientRepo.save(Client.builder().agency(agency).companyName("Client")
                .email("client-" + UUID.randomUUID() + "@example.test").build());
        // The same client email in another tenant must not grant access there.
        Client otherClient = clientRepo.save(Client.builder().agency(otherAgency).companyName("Other client")
                .email(client.getEmail()).build());
        project = project(client, "Logged work");
        project.setTeamMembers(new ArrayList<>(List.of(owner, secondUser)));
        emptyProject = project(client, "No logged work");
        otherProject = project(otherClient, "Other tenant work");
        Task task = taskRepo.save(Task.builder().project(project).title("Work").build());
        completedEntry(task, owner, 90);
        completedEntry(task, secondUser, 1);
        completedEntry(task, noRateUser, 60);
        completedEntry(task, owner, 0);
        completedEntry(task, owner, -10);
        timeEntryRepo.save(TimeEntry.builder().task(task).user(owner)
                .startTime(LocalDateTime.now()).build());
        // An unfinished entry must be excluded even if it has a duration.
        timeEntryRepo.save(TimeEntry.builder().task(task).user(secondUser)
                .startTime(LocalDateTime.now()).durationMinutes(60).build());
        Task otherTask = taskRepo.save(Task.builder().project(otherProject).title("Other work").build());
        completedEntry(otherTask, user(otherAgency, 999.0), 60);
        timeEntryRepo.flush();
        clearInvocations(timeEntryRepo);
    }

    @Test
    void aggregatesCompletedTimeAtEachUsersRateInOneQueryForTheList() {
        List<ProjectDto> projects = projectService.getProjectsForUser(principal(Role.OWNER, owner.getEmail()));

        assertEquals(2, projects.size());
        ProjectDto logged = projects.stream().filter(p -> p.getId().equals(project.getId())).findFirst().orElseThrow();
        // 90 / 60 * 120 + 1 / 60 * 100.5 = 181.675, rounded HALF_UP once.
        assertEquals(new BigDecimal("181.68"), logged.getLoggedValue());
        ProjectDto empty = projects.stream().filter(p -> p.getId().equals(emptyProject.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.00"), empty.getLoggedValue());
        verify(timeEntryRepo, times(1)).sumLoggedValueByProjectIds(eq(agency.getId()), anyCollection());
        assertEquals(new BigDecimal("181.68"), projectService.getProjectById(project.getId(),
                principal(Role.OWNER, owner.getEmail())).getLoggedValue());
    }

    @Test
    void staffMembersDoNotSeeLoggedValueJustAsTheyDoNotSeeBudget() {
        taskRepo.save(Task.builder().project(project).title("Assigned").assignedTo(owner).build());
        List<ProjectDto> projects = projectService.getProjectsForUser(principal(Role.MEMBER, owner.getEmail()));

        assertFalse(projects.isEmpty());
        projects.forEach(p -> {
            assertNull(p.getBudget());
            assertNull(p.getLoggedValue());
        });
    }

    @Test
    void clientSeesOwnProjectTotalsWithoutIndividualRatesOrOtherTenants() {
        Client unrelatedClient = clientRepo.save(Client.builder().agency(agency).companyName("Unrelated")
                .email("unrelated-" + UUID.randomUUID() + "@example.test").build());
        Project unrelatedProject = project(unrelatedClient, "Another client's work");
        UserPrincipal viewer = principal(Role.CLIENT, client.getEmail());
        List<ProjectDto> projects = projectService.getProjectsForUser(viewer);

        assertEquals(2, projects.size());
        ProjectDto logged = projects.stream().filter(p -> p.getId().equals(project.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("181.68"), logged.getLoggedValue());
        assertEquals(2, logged.getTeamMembers().size());
        logged.getTeamMembers().forEach(member -> assertNull(member.getHourlyRate()));
        assertEquals(new BigDecimal("181.68"), projectService.getProjectById(project.getId(), viewer).getLoggedValue());
        assertThrows(IllegalArgumentException.class, () -> projectService.getProjectById(otherProject.getId(), viewer));
        assertThrows(IllegalArgumentException.class, () -> projectService.getProjectById(unrelatedProject.getId(), viewer));

        // Verify the aggregate query's own agency predicate, even with a foreign project ID supplied.
        List<Object[]> rows = timeEntryRepo.sumLoggedValueByProjectIds(agency.getId(),
                List.of(project.getId(), otherProject.getId()));
        assertEquals(1, rows.size());
        assertEquals(project.getId(), rows.getFirst()[0]);
    }

    private AppUser user(Agency userAgency, Double rate) {
        AppUser user = AppUser.builder().agency(userAgency).name("Worker")
                .email(UUID.randomUUID() + "@example.test").passwordHash("").role(Role.OWNER).build();
        user.setHourlyRate(rate);
        return userRepo.save(user);
    }

    private Project project(Client projectClient, String title) {
        return projectRepo.save(Project.builder().client(projectClient).title(title).budget(new BigDecimal("1000")).build());
    }

    private void completedEntry(Task task, AppUser user, int minutes) {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        timeEntryRepo.save(TimeEntry.builder().task(task).user(user).startTime(start)
                .endTime(start.plusMinutes(Math.max(0, minutes))).durationMinutes(minutes).build());
    }

    private UserPrincipal principal(Role role, String email) {
        return new UserPrincipal(owner.getId(), owner.getName(), email, owner.getPasswordHash(),
                agency.getId(), role, List.of());
    }
}
