package com.freelancesuite.dto;

import com.freelancesuite.entity.enums.Role;

public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Double hourlyRate;
    private Long agencyId;

    public UserDto() {}

    public UserDto(Long id, String name, String email, Role role, Double hourlyRate, Long agencyId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.hourlyRate = hourlyRate;
        this.agencyId = agencyId;
    }

    public static UserDtoBuilder builder() { return new UserDtoBuilder(); }

    public static class UserDtoBuilder {
        private Long id;
        private String name;
        private String email;
        private Role role;
        private Double hourlyRate;
        private Long agencyId;

        public UserDtoBuilder id(Long id) { this.id = id; return this; }
        public UserDtoBuilder name(String name) { this.name = name; return this; }
        public UserDtoBuilder email(String email) { this.email = email; return this; }
        public UserDtoBuilder role(Role role) { this.role = role; return this; }
        public UserDtoBuilder hourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; return this; }
        public UserDtoBuilder agencyId(Long agencyId) { this.agencyId = agencyId; return this; }

        public UserDto build() {
            return new UserDto(id, name, email, role, hourlyRate, agencyId);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }
    public Long getAgencyId() { return agencyId; }
    public void setAgencyId(Long agencyId) { this.agencyId = agencyId; }
}
