package com.freelancesuite.dto;

import com.freelancesuite.entity.enums.Role;
import java.time.LocalDateTime;

public class PendingInvitationDto {
    private Long id;
    private String email;
    private Role role;
    private LocalDateTime expiresAt;
    public PendingInvitationDto() {}
    public PendingInvitationDto(Long id, String email, Role role, LocalDateTime expiresAt) {
        this.id = id; this.email = email; this.role = role; this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
