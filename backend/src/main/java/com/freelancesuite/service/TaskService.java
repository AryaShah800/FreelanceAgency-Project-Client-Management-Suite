package com.freelancesuite.service;

import com.freelancesuite.dto.TaskDto;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.Task;
import com.freelancesuite.entity.enums.TaskStatus;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.repository.TaskCommentRepository;
import com.freelancesuite.repository.TaskRepository;
import com.freelancesuite.repository.TimeEntryRepository;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskCommentRepository taskCommentRepository;
    private final TimeEntryRepository timeEntryRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, AppUserRepository userRepository, SimpMessagingTemplate messagingTemplate, TaskCommentRepository taskCommentRepository, TimeEntryRepository timeEntryRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.taskCommentRepository = taskCommentRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByProject(Long projectId, UserPrincipal userPrincipal) {
        Project project = projectRepository.findByIdAndAgencyId(projectId, userPrincipal.getAgencyId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        boolean isClient = userPrincipal != null && userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient && !project.getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access this project's tasks");
        }

        if (userPrincipal.getRole() == com.freelancesuite.entity.enums.Role.MEMBER
                && !taskRepository.existsByProjectIdAndAssignedToId(projectId, userPrincipal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("This project has no work assigned to you.");
        }
        List<Task> tasks = userPrincipal.getRole() == com.freelancesuite.entity.enums.Role.MEMBER
                ? taskRepository.findByProjectIdAndAssignedToId(projectId, userPrincipal.getId())
                : taskRepository.findByProjectId(projectId);
        if (isClient) {
            tasks = tasks.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsClientVisible()))
                    .collect(Collectors.toList());
        }

        return tasks.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksForUser(UserPrincipal userPrincipal) {
        List<Task> tasks;
        boolean isClient = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient) {
            tasks = taskRepository.findClientVisibleTasksByAgencyIdAndClientEmailIgnoreCase(
                    userPrincipal.getAgencyId(), userPrincipal.getEmail());
        } else if (userPrincipal.getRole() == com.freelancesuite.entity.enums.Role.MEMBER) {
            tasks = taskRepository.findByAssignedToId(userPrincipal.getId());
        } else {
            tasks = taskRepository.findByAgencyId(userPrincipal.getAgencyId());
        }

        return tasks.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public TaskDto createTask(TaskDto dto, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(dto.getProjectId(), agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        AppUser assignee = null;
        if (dto.getAssignedToId() != null) {
            assignee = userRepository.findById(dto.getAssignedToId())
                    .filter(user -> user.getAgency().getId().equals(agencyId))
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found in this agency"));
        }

        Task task = Task.builder()
                .project(project)
                .assignedTo(assignee)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : TaskStatus.TODO)
                .estimatedHours(dto.getEstimatedHours() != null ? dto.getEstimatedHours() : 0.0)
                .actualHours(dto.getActualHours() != null ? dto.getActualHours() : 0.0)
                .isClientVisible(dto.getIsClientVisible() != null ? dto.getIsClientVisible() : true)
                .build();

        task = taskRepository.save(task);
        TaskDto result = mapToDto(task);

        messagingTemplate.convertAndSend("/topic/project/" + project.getId(), result);

        return result;
    }

    @Transactional
    public TaskDto updateTask(Long id, TaskDto dto, Long agencyId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (!task.getProject().getClient().getAgency().getId().equals(agencyId)) {
            throw new IllegalArgumentException("Unauthorized task access");
        }

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        if (dto.getEstimatedHours() != null) {
            task.setEstimatedHours(dto.getEstimatedHours());
        }
        if (dto.getActualHours() != null) {
            task.setActualHours(dto.getActualHours());
        }
        if (dto.getIsClientVisible() != null) {
            task.setIsClientVisible(dto.getIsClientVisible());
        }
        if (dto.getAssignedToId() != null) {
            AppUser assignee = userRepository.findById(dto.getAssignedToId())
                    .filter(user -> user.getAgency().getId().equals(agencyId))
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found in this agency"));
            task.setAssignedTo(assignee);
        } else {
            task.setAssignedTo(null);
        }

        task = taskRepository.save(task);
        TaskDto result = mapToDto(task);

        messagingTemplate.convertAndSend("/topic/project/" + task.getProject().getId(), result);

        return result;
    }

    @Transactional
    public TaskDto updateTask(Long id, TaskDto dto, UserPrincipal userPrincipal) {
        if (userPrincipal.getRole() == com.freelancesuite.entity.enums.Role.OWNER) {
            return updateTask(id, dto, userPrincipal.getAgencyId());
        }
        if (userPrincipal.getRole() != com.freelancesuite.entity.enums.Role.MEMBER) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot update this task.");
        }
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        if (!task.getProject().getClient().getAgency().getId().equals(userPrincipal.getAgencyId())
                || task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(userPrincipal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("This task is not assigned to you.");
        }
        if (dto.getStatus() != null) task.setStatus(dto.getStatus());
        task = taskRepository.save(task);
        TaskDto result = mapToDto(task);
        messagingTemplate.convertAndSend("/topic/project/" + task.getProject().getId(), result);
        return result;
    }

    @Transactional
    public void deleteTask(Long id, Long agencyId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (!task.getProject().getClient().getAgency().getId().equals(agencyId)) {
            throw new IllegalArgumentException("Unauthorized task access");
        }

        Long projectId = task.getProject().getId();

        long commentCount = taskCommentRepository.countByTaskId(id);
        if (commentCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete task with " + commentCount + " existing comment(s). Delete them first.");
        }

        long timeEntryCount = timeEntryRepository.countByTaskId(id);
        if (timeEntryCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete task with " + timeEntryCount + " existing time entry(s). Delete them first.");
        }

        taskRepository.delete(task);

        messagingTemplate.convertAndSend("/topic/project/" + projectId, "DELETE:" + id);
    }

    private TaskDto mapToDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToName(task.getAssignedTo() != null ? task.getAssignedTo().getName() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .isClientVisible(task.getIsClientVisible())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
