package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agencies")
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String gstin;

    /** Permanent Account Number (Income Tax). */
    private String pan;

    /** Registered office address for contract party identification. */
    @Column(columnDefinition = "TEXT")
    private String registeredAddress;

    /**
     * Two-digit GST state code (e.g., "27" for Maharashtra, "29" for Karnataka).
     * Used for Place of Supply determination under IGST Act Chapter V.
     * Derived from GSTIN prefix when GSTIN is present; can be set independently
     * for agencies without GST registration.
     */
    private String stateCode;

    // --- Bank details for contract payment terms ---
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;

    @Column(nullable = false)
    private boolean gstRegistered = true;

    private String taxState = "Maharashtra";

    @Column(length = 255)
    private String lutDetails = "";

    @Column(columnDefinition = "TEXT")
    private String legalTemplate = "Intellectual Property: All bespoke source code, designs, and assets created under this agreement will be fully transferred to the client upon receipt of final payment.\n"
            + "Revisions & Acceptance: Each milestone includes up to 2 rounds of minor revisions. Additional structural changes will be scoped via a formal Change Request at standard hourly rates.\n"
            + "Payment Terms: Invoices are due within 7 days of issuance. Late payments may accrue a penalty of 1.5% per month.\n"
            + "Warranty: A 30-day bug-fix warranty period commences upon final production deployment for all delivered features.\n"
            + "Confidentiality: Both parties agree to a mutual Non-Disclosure Agreement (NDA) regarding all proprietary business data and trade secrets.\n"
            + "Governing Law & Jurisdiction: This agreement shall be governed by the laws of India, and the courts of Maharashtra shall have exclusive jurisdiction.\n"
            + "Termination: Either party may terminate this agreement with 15 days written notice.\n"
            + "Limitation of Liability: The agency's liability is capped at the total amount paid under this agreement.";
    @Column(nullable = false)
    private boolean proposalDefaultsInitialized;

    private String subscriptionPlan = "PRO";

    private LocalDateTime createdAt;

    public Agency() {}

    public Agency(Long id, String name, String gstin, String pan, String registeredAddress,
                  String stateCode, String bankName, String bankAccountNumber, String bankIfsc,
                  String subscriptionPlan, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.gstin = gstin;
        this.pan = pan;
        this.registeredAddress = registeredAddress;
        this.stateCode = stateCode;
        this.bankName = bankName;
        this.bankAccountNumber = bankAccountNumber;
        this.bankIfsc = bankIfsc;
        this.subscriptionPlan = subscriptionPlan != null ? subscriptionPlan : "PRO";
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static AgencyBuilder builder() {
        return new AgencyBuilder();
    }

    public static class AgencyBuilder {
        private Long id;
        private String name;
        private String gstin;
        private String pan;
        private String registeredAddress;
        private String stateCode;
        private String bankName;
        private String bankAccountNumber;
        private String bankIfsc;
        private String subscriptionPlan = "PRO";
        private LocalDateTime createdAt;

        public AgencyBuilder id(Long id) { this.id = id; return this; }
        public AgencyBuilder name(String name) { this.name = name; return this; }
        public AgencyBuilder gstin(String gstin) { this.gstin = gstin; return this; }
        public AgencyBuilder pan(String pan) { this.pan = pan; return this; }
        public AgencyBuilder registeredAddress(String registeredAddress) { this.registeredAddress = registeredAddress; return this; }
        public AgencyBuilder stateCode(String stateCode) { this.stateCode = stateCode; return this; }
        public AgencyBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public AgencyBuilder bankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; return this; }
        public AgencyBuilder bankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; return this; }
        public AgencyBuilder subscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; return this; }
        public AgencyBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Agency build() {
            return new Agency(id, name, gstin, pan, registeredAddress, stateCode,
                    bankName, bankAccountNumber, bankIfsc, subscriptionPlan, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public boolean isGstRegistered() { return gstRegistered; }
    public void setGstRegistered(boolean gstRegistered) { this.gstRegistered = gstRegistered; }
    public String getTaxState() { return taxState; }
    public void setTaxState(String taxState) { this.taxState = taxState; }
    public String getLutDetails() { return lutDetails; }
    public void setLutDetails(String lutDetails) { this.lutDetails = lutDetails; }
    public String getLegalTemplate() { return legalTemplate; }
    public void setLegalTemplate(String legalTemplate) { this.legalTemplate = legalTemplate; }
    public boolean isProposalDefaultsInitialized() { return proposalDefaultsInitialized; }
    public void setProposalDefaultsInitialized(boolean proposalDefaultsInitialized) { this.proposalDefaultsInitialized = proposalDefaultsInitialized; }
    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
