package com.freelancesuite.service;

import com.freelancesuite.dto.ProjectDto;
import com.freelancesuite.dto.UserDto;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.ClientRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, ClientRepository clientRepository) {
        this.projectRepository = projectRepository;
        this.clientRepository = clientRepository;
    }

    public List<ProjectDto> getProjectsForUser(UserPrincipal userPrincipal) {
        List<Project> projects;
        boolean isClient = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient) {
            // Client Portal: Only show projects belonging to this client's email
            projects = projectRepository.findByAgencyId(userPrincipal.getAgencyId())
                    .stream()
                    .filter(p -> p.getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail()))
                    .collect(Collectors.toList());
        } else {
            // Owner / Member: Show all agency projects
            projects = projectRepository.findByAgencyId(userPrincipal.getAgencyId());
        }

        return projects.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ProjectDto getProjectById(Long id, UserPrincipal userPrincipal) {
        Project project = projectRepository.findByIdAndAgencyId(id, userPrincipal.getAgencyId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        boolean isClient = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient && !project.getClient().getEmail().equalsIgnoreCase(userPrincipal.getEmail())) {
            throw new IllegalArgumentException("Unauthorized project access");
        }

        return mapToDto(project);
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
        projectRepository.delete(project);
    }

    private ProjectDto mapToDto(Project project) {
        List<UserDto> members = project.getTeamMembers().stream().map(u -> UserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .hourlyRate(u.getHourlyRate())
                .build()).collect(Collectors.toList());

        return ProjectDto.builder()
                .id(project.getId())
                .clientId(project.getClient().getId())
                .clientName(project.getClient().getCompanyName())
                .title(project.getTitle())
                .description(project.getDescription())
                .budget(project.getBudget())
                .deadline(project.getDeadline())
                .teamMembers(members)
                .createdAt(project.getCreatedAt())
                .build();
    }
}
