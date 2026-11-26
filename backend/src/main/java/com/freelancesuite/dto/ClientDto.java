package com.freelancesuite.dto;


import jakarta.validation.constraints.*;
import com.freelancesuite.entity.enums.DealStage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ClientDto {
    private Long id;

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must be 255 characters or fewer")
    private String companyName;

    @Size(max = 255, message = "Contact person must be 255 characters or fewer")
    private String contactPerson;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 32, message = "Phone must be 32 characters or fewer")
    private String phone;
    // Optional; when given it must be a 15-character GSTIN (state code, PAN, entity, Z, checksum).
    @Pattern(regexp = "^$|^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z][Zz][0-9A-Za-z]$",
            message = "GSTIN must be 15 characters, e.g. 27ABCDE1234F1Z5")
    private String gstin;
    private DealStage dealStage;
    private LocalDateTime createdAt;

    public ClientDto() {}

    public ClientDto(Long id, String companyName, String contactPerson, String email, String phone, String gstin, DealStage dealStage, LocalDateTime createdAt) {
        this.id = id;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.gstin = gstin;
        this.dealStage = dealStage;
        this.createdAt = createdAt;
    }

    public static ClientDtoBuilder builder() { return new ClientDtoBuilder(); }

    public static class ClientDtoBuilder {
        private Long id;
        private String companyName;
        private String contactPerson;
        private String email;
        private String phone;
        private String gstin;
        private DealStage dealStage;
        private LocalDateTime createdAt;

        public ClientDtoBuilder id(Long id) { this.id = id; return this; }
        public ClientDtoBuilder companyName(String companyName) { this.companyName = companyName; return this; }
        public ClientDtoBuilder contactPerson(String contactPerson) { this.contactPerson = contactPerson; return this; }
        public ClientDtoBuilder email(String email) { this.email = email; return this; }
        public ClientDtoBuilder phone(String phone) { this.phone = phone; return this; }
        public ClientDtoBuilder gstin(String gstin) { this.gstin = gstin; return this; }
        public ClientDtoBuilder dealStage(DealStage dealStage) { this.dealStage = dealStage; return this; }
        public ClientDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ClientDto build() {
            return new ClientDto(id, companyName, contactPerson, email, phone, gstin, dealStage, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public DealStage getDealStage() { return dealStage; }
    public void setDealStage(DealStage dealStage) { this.dealStage = dealStage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
