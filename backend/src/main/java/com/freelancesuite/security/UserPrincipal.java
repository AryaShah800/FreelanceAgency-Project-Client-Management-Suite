package com.freelancesuite.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.freelancesuite.entity.AppUser;
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
    private Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String name, String email, String password, Long agencyId, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.agencyId = agencyId;
        this.authorities = authorities;
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
            authorities
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Long getAgencyId() { return agencyId; }

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
        return true;
    }
}
