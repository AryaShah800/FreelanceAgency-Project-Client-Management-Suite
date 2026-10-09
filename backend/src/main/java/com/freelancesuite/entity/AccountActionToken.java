package com.freelancesuite.entity;

import com.freelancesuite.entity.enums.AccountTokenPurpose;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_action_tokens")
public class AccountActionToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountTokenPurpose purpose;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AccountActionToken() {}
    public AccountActionToken(AppUser user, String tokenHash, AccountTokenPurpose purpose,
                              LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.user = user; this.tokenHash = tokenHash; this.purpose = purpose;
        this.expiresAt = expiresAt; this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public AccountTokenPurpose getPurpose() { return purpose; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
