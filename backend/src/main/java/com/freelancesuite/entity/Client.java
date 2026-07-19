package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.DealStage;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(nullable = false)
    private String companyName;

    private String contactPerson;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String gstin;

    @Enumerated(EnumType.STRING)
    private DealStage dealStage = DealStage.LEAD;

    private LocalDateTime createdAt;

    public Client() {}

    public Client(Long id, Agency agency, String companyName, String contactPerson, String email, String phone, String gstin, DealStage dealStage, LocalDateTime createdAt) {
        this.id = id;
        this.agency = agency;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.gstin = gstin;
        this.dealStage = dealStage != null ? dealStage : DealStage.LEAD;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static ClientBuilder builder() { return new ClientBuilder(); }

    public static class ClientBuilder {
        private Long id;
        private Agency agency;
        private String companyName;
        private String contactPerson;
        private String email;
        private String phone;
        private String gstin;
        private DealStage dealStage = DealStage.LEAD;
        private LocalDateTime createdAt;

        public ClientBuilder id(Long id) { this.id = id; return this; }
        public ClientBuilder agency(Agency agency) { this.agency = agency; return this; }
        public ClientBuilder companyName(String companyName) { this.companyName = companyName; return this; }
        public ClientBuilder contactPerson(String contactPerson) { this.contactPerson = contactPerson; return this; }
        public ClientBuilder email(String email) { this.email = email; return this; }
        public ClientBuilder phone(String phone) { this.phone = phone; return this; }
        public ClientBuilder gstin(String gstin) { this.gstin = gstin; return this; }
        public ClientBuilder dealStage(DealStage dealStage) { this.dealStage = dealStage; return this; }
        public ClientBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Client build() {
            return new Client(id, agency, companyName, contactPerson, email, phone, gstin, dealStage, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Agency getAgency() { return agency; }
    public void setAgency(Agency agency) { this.agency = agency; }
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
