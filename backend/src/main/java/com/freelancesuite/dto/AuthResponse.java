package com.freelancesuite.dto;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private String role;
    private Long agencyId;
    private String agencyName;

    public AuthResponse() {}

    public AuthResponse(String token, String type, Long userId, String name, String email, String role, Long agencyId, String agencyName) {
        this.token = token;
        this.type = type != null ? type : "Bearer";
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.agencyId = agencyId;
        this.agencyName = agencyName;
    }

    public static AuthResponseBuilder builder() { return new AuthResponseBuilder(); }

    public static class AuthResponseBuilder {
        private String token;
        private String type = "Bearer";
        private Long userId;
        private String name;
        private String email;
        private String role;
        private Long agencyId;
        private String agencyName;

        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder type(String type) { this.type = type; return this; }
        public AuthResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public AuthResponseBuilder name(String name) { this.name = name; return this; }
        public AuthResponseBuilder email(String email) { this.email = email; return this; }
        public AuthResponseBuilder role(String role) { this.role = role; return this; }
        public AuthResponseBuilder agencyId(Long agencyId) { this.agencyId = agencyId; return this; }
        public AuthResponseBuilder agencyName(String agencyName) { this.agencyName = agencyName; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, type, userId, name, email, role, agencyId, agencyName);
        }
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getAgencyId() { return agencyId; }
    public void setAgencyId(Long agencyId) { this.agencyId = agencyId; }
    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }
}
