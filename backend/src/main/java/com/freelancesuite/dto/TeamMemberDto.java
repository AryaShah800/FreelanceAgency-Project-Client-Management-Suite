package com.freelancesuite.dto;

import com.freelancesuite.entity.enums.Role;

public class TeamMemberDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
    public TeamMemberDto() {}
    public TeamMemberDto(Long id, String name, String email, Role role, boolean enabled) {
        this.id = id; this.name = name; this.email = email; this.role = role; this.enabled = enabled;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
}
