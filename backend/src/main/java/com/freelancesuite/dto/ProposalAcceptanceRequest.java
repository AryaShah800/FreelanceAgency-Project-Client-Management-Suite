package com.freelancesuite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProposalAcceptanceRequest {
    @NotBlank
    @Size(max = 160)
    private String signerName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String signerEmail;

    public ProposalAcceptanceRequest() {}

    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }
    public String getSignerEmail() { return signerEmail; }
    public void setSignerEmail(String signerEmail) { this.signerEmail = signerEmail; }
}
