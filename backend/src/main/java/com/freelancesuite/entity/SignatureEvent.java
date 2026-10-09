package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.AuthMethod;
import com.freelancesuite.entity.enums.SignatureType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistence model for signer activity telemetry. Recording these fields alone
 * does not verify signer identity, make this entity append-only, or establish
 * legal admissibility.
 */
@Entity
@Table(name = "signature_events")
public class SignatureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_signer_id", nullable = false)
    private ContractSigner contractSigner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_version_id", nullable = false)
    private ContractVersion contractVersion;

    @Column(nullable = false)
    private LocalDateTime signedAt;

    /** IPv4 or IPv6 address of the signer at execution time. */
    @Column(nullable = false)
    private String ipAddress;

    /** HTTP User-Agent header of the signer's browser/client. */
    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthMethod authMethod;

    /**
     * The full consent statement acknowledged by the signer, e.g.:
     * "I, [Name], hereby acknowledge that I have read and agree to the terms
     * of this agreement dated [Date]. IP: [IP]. Timestamp: [UTC]."
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String consentStatement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignatureType signatureType;

    /**
     * OTP verification token digest (SHA-256 of the OTP used), for audit
     * trail purposes. Null for non-OTP auth methods.
     */
    private String otpTokenDigest;

    public SignatureEvent() {}

    @PrePersist
    protected void onCreate() {
        if (signedAt == null) signedAt = LocalDateTime.now();
    }

    public static SignatureEventBuilder builder() { return new SignatureEventBuilder(); }

    public static class SignatureEventBuilder {
        private final SignatureEvent e = new SignatureEvent();

        public SignatureEventBuilder contractSigner(ContractSigner v) { e.contractSigner = v; return this; }
        public SignatureEventBuilder contractVersion(ContractVersion v) { e.contractVersion = v; return this; }
        public SignatureEventBuilder signedAt(LocalDateTime v) { e.signedAt = v; return this; }
        public SignatureEventBuilder ipAddress(String v) { e.ipAddress = v; return this; }
        public SignatureEventBuilder userAgent(String v) { e.userAgent = v; return this; }
        public SignatureEventBuilder authMethod(AuthMethod v) { e.authMethod = v; return this; }
        public SignatureEventBuilder consentStatement(String v) { e.consentStatement = v; return this; }
        public SignatureEventBuilder signatureType(SignatureType v) { e.signatureType = v; return this; }
        public SignatureEventBuilder otpTokenDigest(String v) { e.otpTokenDigest = v; return this; }

        public SignatureEvent build() { return e; }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ContractSigner getContractSigner() { return contractSigner; }
    public void setContractSigner(ContractSigner contractSigner) { this.contractSigner = contractSigner; }
    public ContractVersion getContractVersion() { return contractVersion; }
    public void setContractVersion(ContractVersion contractVersion) { this.contractVersion = contractVersion; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public AuthMethod getAuthMethod() { return authMethod; }
    public void setAuthMethod(AuthMethod authMethod) { this.authMethod = authMethod; }
    public String getConsentStatement() { return consentStatement; }
    public void setConsentStatement(String consentStatement) { this.consentStatement = consentStatement; }
    public SignatureType getSignatureType() { return signatureType; }
    public void setSignatureType(SignatureType signatureType) { this.signatureType = signatureType; }
    public String getOtpTokenDigest() { return otpTokenDigest; }
    public void setOtpTokenDigest(String otpTokenDigest) { this.otpTokenDigest = otpTokenDigest; }
}
