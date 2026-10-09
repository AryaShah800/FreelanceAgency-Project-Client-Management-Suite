package com.freelancesuite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AgencyProfileDto {
    @NotBlank
    @Size(max = 150)
    private String name;
    @Pattern(regexp = "^$|^[0-9]{2}[A-Za-z0-9]{13}$", message = "GSTIN must contain 15 characters")
    private String gstin;
    @Pattern(regexp = "^$|^[A-Za-z]{5}[0-9]{4}[A-Za-z]$", message = "PAN must have 10 characters")
    private String pan;
    @Size(max = 2000)
    private String registeredAddress;
    @Pattern(regexp = "^$|^[0-9]{2}$", message = "State code must contain two digits")
    private String stateCode;
    @Size(max = 120)
    private String bankName;
    @Size(max = 80)
    private String bankAccountNumber;
    @Pattern(regexp = "^$|^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "IFSC must be 11 characters")
    private String bankIfsc;

    public AgencyProfileDto() {}
    public AgencyProfileDto(String name, String gstin, String pan, String registeredAddress,
                            String stateCode, String bankName, String bankAccountNumber, String bankIfsc) {
        this.name = name; this.gstin = gstin; this.pan = pan; this.registeredAddress = registeredAddress;
        this.stateCode = stateCode; this.bankName = bankName; this.bankAccountNumber = bankAccountNumber;
        this.bankIfsc = bankIfsc;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public String getRegisteredAddress() { return registeredAddress; }
    public void setRegisteredAddress(String registeredAddress) { this.registeredAddress = registeredAddress; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }
}
