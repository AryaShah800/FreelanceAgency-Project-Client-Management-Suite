package com.freelancesuite.service;

import com.freelancesuite.dto.TaskDto;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.Task;
import com.freelancesuite.entity.enums.TaskStatus;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.repository.TaskRepository;
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

    @Autowired
    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, AppUserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<TaskDto> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<TaskDto> getAllAgencyTasks(Long agencyId) {
        return taskRepository.findByAgencyId(agencyId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public TaskDto createTask(TaskDto dto, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(dto.getProjectId(), agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        AppUser assignee = null;
        if (dto.getAssignedToId() != null) {
            assignee = userRepository.findById(dto.getAssignedToId()).orElse(null);
        }

        Task task = Task.builder()
                .project(project)
                .assignedTo(assignee)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : TaskStatus.TODO)
                .estimatedHours(dto.getEstimatedHours() != null ? dto.getEstimatedHours() : 0.0)
                .actualHours(dto.getActualHours() != null ? dto.getActualHours() : 0.0)
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
        if (dto.getAssignedToId() != null) {
            AppUser assignee = userRepository.findById(dto.getAssignedToId()).orElse(null);
            task.setAssignedTo(assignee);
        }

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
                .createdAt(task.getCreatedAt())
                .build();
    }
}
