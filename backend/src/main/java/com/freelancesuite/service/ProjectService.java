package com.freelancesuite.service;

import com.freelancesuite.dto.ProjectDto;
import com.freelancesuite.dto.UserDto;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.ClientRepository;
import com.freelancesuite.repository.ExpenseRepository;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.repository.TaskRepository;
import com.freelancesuite.repository.TimeEntryRepository;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final TaskRepository taskRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final TimeEntryRepository timeEntryRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, ClientRepository clientRepository,
                          TaskRepository taskRepository, InvoiceRepository invoiceRepository,
                          ExpenseRepository expenseRepository, TimeEntryRepository timeEntryRepository) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
        this.taskRepository = taskRepository;
        this.invoiceRepository = invoiceRepository;
        this.expenseRepository = expenseRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsForUser(UserPrincipal userPrincipal) {
        List<Project> projects;
        boolean isClient = userPrincipal.getRole() == Role.CLIENT;

        if (isClient) {
            // Client Portal: Use optimized DB query to prevent N+1 and OOM
            projects = projectRepository.findByAgencyIdAndClientEmailIgnoreCase(
                    userPrincipal.getAgencyId(), userPrincipal.getEmail());
        } else if (userPrincipal.getRole() == Role.MEMBER) {
            projects = projectRepository.findAssignedProjectsByAgencyAndUser(
                    userPrincipal.getAgencyId(), userPrincipal.getId());
        } else {
            projects = projectRepository.findByAgencyId(userPrincipal.getAgencyId());
        }

        Map<Long, BigDecimal> loggedValues = getLoggedValues(projects, userPrincipal.getAgencyId());
        return projects.stream().map(project -> mapToDto(project, userPrincipal.getRole(),
                loggedValues.getOrDefault(project.getId(), BigDecimal.ZERO.setScale(2)))).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id, UserPrincipal userPrincipal) {
        Project project = projectRepository.findByIdAndAgencyId(id, userPrincipal.getAgencyId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        boolean isClient = userPrincipal.getRole() == Role.CLIENT;

        if (isClient && !project.getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail())) {
            throw new IllegalArgumentException("Unauthorized project access");
        }
        if (userPrincipal.getRole() == Role.MEMBER
                && !taskRepository.existsByProjectIdAndAssignedToId(id, userPrincipal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("This project has no work assigned to you.");
        }

        return mapToDto(project, userPrincipal.getRole());
    }

    @Transactional
    public ProjectDto createProject(ProjectDto dto, Long agencyId) {
        Client client = clientRepository.findByIdAndAgencyId(dto.getClientId(), agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        Project project = Project.builder()
                .client(client)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .budget(dto.getBudget())
                .deadline(dto.getDeadline())
                .build();

        return mapToDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectDto updateProject(Long id, ProjectDto dto, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        project.setTitle(dto.getTitle());
        project.setDescription(dto.getDescription());
        project.setBudget(dto.getBudget());
        project.setDeadline(dto.getDeadline());

        return mapToDto(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        long taskCount = taskRepository.countByProjectId(id);
        if (taskCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete project with " + taskCount + " existing task(s). Delete them first.");
        }

        long invoiceCount = invoiceRepository.countByProjectId(id);
        if (invoiceCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete project with " + invoiceCount + " existing invoice(s). Delete them first.");
        }

        long expenseCount = expenseRepository.countByProjectId(id);
        if (expenseCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete project with " + expenseCount + " existing expense(s). Delete them first.");
        }

        long timeEntryCount = timeEntryRepository.countByTaskProjectId(id);
        if (timeEntryCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete project with " + timeEntryCount + " existing time entry(s). Delete them first.");
        }

        projectRepository.delete(project);
    }

    private ProjectDto mapToDto(Project project) {
        return mapToDto(project, Role.OWNER);
    }

    private ProjectDto mapToDto(Project project, Role viewerRole) {
        Map<Long, BigDecimal> loggedValues = getLoggedValues(List.of(project), project.getClient().getAgency().getId());
        return mapToDto(project, viewerRole, loggedValues.getOrDefault(project.getId(), BigDecimal.ZERO.setScale(2)));
    }

    private Map<Long, BigDecimal> getLoggedValues(List<Project> projects, Long agencyId) {
        if (projects.isEmpty()) {
            return Map.of();
        }
        List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        return timeEntryRepository.sumLoggedValueByProjectIds(agencyId, projectIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row ->
                        BigDecimal.valueOf(((Number) row[1]).doubleValue()).setScale(2, RoundingMode.HALF_UP)));
    }

    private ProjectDto mapToDto(Project project, Role viewerRole, BigDecimal loggedValue) {
        List<UserDto> members = project.getTeamMembers().stream().map(u -> UserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .role(u.getRole())
                .build()).collect(Collectors.toList());

        return ProjectDto.builder()
                .id(project.getId())
                .clientId(project.getClient().getId())
                .clientName(project.getClient().getCompanyName())
                .title(project.getTitle())
                .description(project.getDescription())
                .budget(viewerRole == Role.MEMBER ? null : project.getBudget())
                // Hidden from staff like the budget: rates x hours would reveal colleagues' hourly rates.
                .loggedValue(viewerRole == Role.MEMBER ? null : loggedValue)
                .deadline(project.getDeadline())
                .teamMembers(members)
                .createdAt(project.getCreatedAt())
                .build();
    }
}
