package com.freelancesuite.dto;

import jakarta.validation.constraints.NotBlank;

public class EmailTokenRequest {
    @NotBlank private String token;
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
