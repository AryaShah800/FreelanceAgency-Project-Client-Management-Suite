package com.freelancesuite.dto;

import com.freelancesuite.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InviteMemberRequest {
    @NotBlank @Email @Size(max = 254) private String email;
    @NotNull private Role role;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
