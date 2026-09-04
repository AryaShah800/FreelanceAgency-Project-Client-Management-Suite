package com.freelancesuite.service;

import com.freelancesuite.dto.TaskCommentDto;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.Task;
import com.freelancesuite.entity.TaskComment;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.repository.TaskCommentRepository;
import com.freelancesuite.repository.TaskRepository;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public TaskCommentService(TaskCommentRepository commentRepository, TaskRepository taskRepository, AppUserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<TaskCommentDto> getCommentsForTask(Long taskId, UserPrincipal userPrincipal) {
        assertTaskAccess(taskId, userPrincipal);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public TaskCommentDto addComment(Long taskId, String content, UserPrincipal userPrincipal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        assertTaskAccess(task, userPrincipal);

        AppUser author = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(author)
                .content(content)
                .build();

        comment = commentRepository.save(comment);
        TaskCommentDto dto = mapToDto(comment);

        // Broadcast comment over WebSocket
        messagingTemplate.convertAndSend("/topic/project/" + task.getProject().getId() + "/comments", dto);

        return dto;
    }

    private TaskCommentDto mapToDto(TaskComment comment) {
        return TaskCommentDto.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .authorRole(comment.getAuthor().getRole().name())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private void assertTaskAccess(Long taskId, UserPrincipal userPrincipal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        assertTaskAccess(task, userPrincipal);
    }

    private void assertTaskAccess(Task task, UserPrincipal userPrincipal) {
        if (!task.getProject().getClient().getAgency().getId().equals(userPrincipal.getAgencyId())) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access comments for this task");
        }
        boolean isClient = userPrincipal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
        if (isClient && (!task.getProject().getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail()) || !Boolean.TRUE.equals(task.getIsClientVisible()))) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access comments for this task");
        }
    }
}
