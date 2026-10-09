package com.freelancesuite.dto;

import jakarta.validation.constraints.Size;

public class ProposalDefaultsDto {
    @Size(max = 150) private String state;
    private boolean isGstRegistered;
    @Size(max = 255) private String lutDetails;
    @Size(max = 12000) private String legalTemplate;
    private boolean initialized;
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public boolean getIsGstRegistered() { return isGstRegistered; }
    public void setIsGstRegistered(boolean gstRegistered) { this.isGstRegistered = gstRegistered; }
    public String getLutDetails() { return lutDetails; }
    public void setLutDetails(String lutDetails) { this.lutDetails = lutDetails; }
    public String getLegalTemplate() { return legalTemplate; }
    public void setLegalTemplate(String legalTemplate) { this.legalTemplate = legalTemplate; }
    public boolean isInitialized() { return initialized; }
    public void setInitialized(boolean initialized) { this.initialized = initialized; }
}
