package com.freelancesuite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AcceptInvitationRequest {
    @NotBlank private String token;
    @NotBlank @Size(max = 150) private String name;
    @NotBlank @Size(min = 12, max = 72) private String password;
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
