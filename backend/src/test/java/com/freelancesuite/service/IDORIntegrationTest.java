package com.freelancesuite.service;

import com.freelancesuite.dto.TaskDto;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.Task;
import com.freelancesuite.entity.enums.DealStage;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.entity.enums.TaskStatus;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.repository.ClientRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.repository.TaskRepository;
import com.freelancesuite.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class IDORIntegrationTest {

    @Autowired private TaskService taskService;
    @Autowired private TimeEntryService timeEntryService;
    @Autowired private ExpenseService expenseService;
    @Autowired private ClientService clientService;
    @Autowired private ProjectService projectService;
    @Autowired private TaskCommentService taskCommentService;

    @Autowired private AgencyRepository agencyRepo;
    @Autowired private AppUserRepository userRepo;
    @Autowired private ClientRepository clientRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TaskRepository taskRepo;

    private Agency agencyA, agencyB;
    private AppUser userA, userB;
    private Client clientA, clientB;
    private Project projectA, projectB;
    private Task taskA, taskB;

    @BeforeEach
    void setup() {
        agencyA = agencyRepo.save(Agency.builder().name("Agency A").build());
        agencyB = agencyRepo.save(Agency.builder().name("Agency B").build());

        String suffix = java.util.UUID.randomUUID().toString();
        userA = userRepo.save(AppUser.builder().agency(agencyA).name("User A").email("a@a.com" + suffix).role(Role.OWNER).passwordHash("").build());
        userB = userRepo.save(AppUser.builder().agency(agencyB).name("User B").email("b@b.com" + suffix).role(Role.OWNER).passwordHash("").build());

        clientA = clientRepo.save(Client.builder().agency(agencyA).companyName("Client A").email("c@a.com" + suffix).dealStage(DealStage.WON).build());
        clientB = clientRepo.save(Client.builder().agency(agencyB).companyName("Client B").email("c@b.com" + suffix).dealStage(DealStage.WON).build());

        projectA = projectRepo.save(Project.builder().client(clientA).title("Project A").budget(BigDecimal.TEN).deadline(LocalDate.now()).build());
        projectB = projectRepo.save(Project.builder().client(clientB).title("Project B").budget(BigDecimal.TEN).deadline(LocalDate.now()).build());

        taskA = taskRepo.save(Task.builder().project(projectA).title("Task A").status(TaskStatus.TODO).build());
        taskB = taskRepo.save(Task.builder().project(projectB).title("Task B").status(TaskStatus.TODO).build());
    }

    private UserPrincipal getPrincipal(AppUser user) {
        return new UserPrincipal(user.getId(), user.getName(), user.getEmail(), user.getPasswordHash(), user.getAgency().getId(), user.getRole(), List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    @Test
    void testClientServiceIDOR() {
        UserPrincipal principalB = getPrincipal(userB);
        assertThrows(IllegalArgumentException.class, () -> clientService.getClientById(clientA.getId(), principalB.getAgencyId()));
    }

    @Test
    void testProjectServiceIDOR() {
        UserPrincipal principalB = getPrincipal(userB);
        assertThrows(IllegalArgumentException.class, () -> projectService.getProjectById(projectA.getId(), principalB));
    }

    @Test
    void testTaskServiceIDOR() {
        UserPrincipal principalB = getPrincipal(userB);
        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(taskA.getId(), new TaskDto(), principalB.getAgencyId()));
    }

    @Test
    void testExpenseServiceIDOR() {
        UserPrincipal principalB = getPrincipal(userB);
        assertThrows(AccessDeniedException.class, () -> expenseService.getExpensesByProject(projectA.getId(), principalB.getAgencyId()));
    }

    @Test
    void testTimeEntryServiceIDOR() {
        UserPrincipal principalB = getPrincipal(userB);
        assertThrows(AccessDeniedException.class, () -> timeEntryService.startTimer(taskA.getId(), principalB));
    }

    @Test
    void testTaskCommentServiceIDOR() {
        UserPrincipal principalB = getPrincipal(userB);
        assertThrows(AccessDeniedException.class, () -> taskCommentService.getCommentsForTask(taskA.getId(), principalB));
    }

    @Test
    void staffCanSeeAndUpdateOnlyTheirAssignedWorkWithinTheirAgency() {
        String suffix = java.util.UUID.randomUUID().toString();
        AppUser assignedStaff = userRepo.save(AppUser.builder().agency(agencyA).name("Assigned Staff")
                .email("assigned-" + suffix + "@example.test").role(Role.MEMBER).passwordHash("").build());
        AppUser otherStaff = userRepo.save(AppUser.builder().agency(agencyA).name("Other Staff")
                .email("other-" + suffix + "@example.test").role(Role.MEMBER).passwordHash("").build());
        Task assignedTask = taskRepo.save(Task.builder().project(projectA).assignedTo(assignedStaff)
                .title("Assigned Task").status(TaskStatus.TODO).build());
        Task otherTask = taskRepo.save(Task.builder().project(projectA).assignedTo(otherStaff)
                .title("Other Task").status(TaskStatus.TODO).build());
        UserPrincipal staff = getPrincipal(assignedStaff);

        assertEquals(List.of(assignedTask.getId()), taskService.getTasksForUser(staff).stream().map(TaskDto::getId).toList());
        assertEquals(List.of(assignedTask.getId()), taskService.getTasksByProject(projectA.getId(), staff)
                .stream().map(TaskDto::getId).toList());
        assertNull(projectService.getProjectById(projectA.getId(), staff).getBudget());

        TaskDto attemptedEdit = new TaskDto();
        attemptedEdit.setTitle("Changed someone else's task");
        attemptedEdit.setStatus(TaskStatus.IN_PROGRESS);
        assertThrows(AccessDeniedException.class,
                () -> taskService.updateTask(otherTask.getId(), attemptedEdit, staff));
        TaskDto update = taskService.updateTask(assignedTask.getId(), attemptedEdit, staff);
        assertEquals("Assigned Task", update.getTitle(), "Staff may update work status but not task details.");
        assertEquals(TaskStatus.IN_PROGRESS, update.getStatus());
    }
}
