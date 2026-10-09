package com.freelancesuite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BootstrapOwnerRequest {
    @NotBlank @Size(max = 150) private String agencyName;
    @NotBlank @Size(max = 150) private String ownerName;
    @NotBlank @Email @Size(max = 254) private String email;
    @NotBlank @Size(min = 12, max = 72) private String password;
    @NotBlank private String bootstrapSecret;
    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getBootstrapSecret() { return bootstrapSecret; }
    public void setBootstrapSecret(String bootstrapSecret) { this.bootstrapSecret = bootstrapSecret; }
}
