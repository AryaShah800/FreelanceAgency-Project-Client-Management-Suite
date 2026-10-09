package com.freelancesuite.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private Long id;
    private String name;
    private String email;
    @JsonIgnore
    private String password;
    private Long agencyId;
    private Role role;
    private boolean enabled;
    private boolean emailVerified;
    private int tokenVersion;
    private Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String name, String email, String password, Long agencyId, Role role,
                         boolean enabled, boolean emailVerified, int tokenVersion,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.agencyId = agencyId;
        this.role = role;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
        this.tokenVersion = tokenVersion;
        this.authorities = authorities;
    }

    /** Compatibility constructor for existing service tests and manual principals. */
    public UserPrincipal(Long id, String name, String email, String password, Long agencyId, Role role,
                         Collection<? extends GrantedAuthority> authorities) {
        this(id, name, email, password, agencyId, role, true, true, 0, authorities);
    }

    public static UserPrincipal create(AppUser user) {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new UserPrincipal(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getAgency().getId(),
            user.getRole(),
            user.isEnabled(),
            user.isEmailVerified(),
            user.getTokenVersion(),
            authorities
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Long getAgencyId() { return agencyId; }
    public Role getRole() { return role; }
    public int getTokenVersion() { return tokenVersion; }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled && emailVerified;
    }
}
