package com.freelancesuite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class RegisterRequest {

    @NotBlank(message = "Agency name is required")
    @Size(max = 150, message = "Agency name must be at most 150 characters")
    private String agencyName;

    @Size(max = 15, message = "GSTIN must be at most 15 characters")
    @Pattern(regexp = "^$|^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z]Z[0-9A-Za-z]$",
            message = "Enter a valid GSTIN or leave it blank")
    private String gstin;

    @NotBlank(message = "Owner name is required")
    @Size(max = 150, message = "Owner name must be at most 150 characters")
    private String ownerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 72, message = "Password must be 12 to 72 characters long")
    private String password;

    public RegisterRequest() {}

    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
