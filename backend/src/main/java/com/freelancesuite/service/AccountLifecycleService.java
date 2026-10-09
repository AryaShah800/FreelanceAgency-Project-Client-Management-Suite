package com.freelancesuite.service;

import com.freelancesuite.dto.*;
import com.freelancesuite.entity.*;
import com.freelancesuite.entity.enums.AccountTokenPurpose;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.*;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.util.Sha256Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class AccountLifecycleService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long ACTION_EMAIL_COOLDOWN_SECONDS = 60;
    private final AgencyRepository agencyRepository;
    private final AppUserRepository userRepository;
    private final AccountActionTokenRepository actionTokenRepository;
    private final AgencyInvitationRepository invitationRepository;
    private final ClientRepository clientRepository;
    private final PlatformStateRepository platformStateRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailDeliveryService emailDelivery;
    @Value("${app.bootstrap.secret:}") private String bootstrapSecret;

    public AccountLifecycleService(AgencyRepository agencyRepository, AppUserRepository userRepository,
                                   AccountActionTokenRepository actionTokenRepository,
                                   AgencyInvitationRepository invitationRepository,
                                   ClientRepository clientRepository,
                                   PlatformStateRepository platformStateRepository,
                                   PasswordEncoder passwordEncoder, EmailDeliveryService emailDelivery) {
        this.agencyRepository = agencyRepository; this.userRepository = userRepository;
        this.actionTokenRepository = actionTokenRepository; this.invitationRepository = invitationRepository;
        this.clientRepository = clientRepository;
        this.platformStateRepository = platformStateRepository; this.passwordEncoder = passwordEncoder;
        this.emailDelivery = emailDelivery;
    }

    @Transactional
    public void bootstrapOwner(BootstrapOwnerRequest request) {
        if (bootstrapSecret == null || bootstrapSecret.length() < 32 || !constantTimeEquals(bootstrapSecret, request.getBootstrapSecret())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "First-owner setup is not available.");
        }
        PlatformState state = platformStateRepository.lockBootstrapState();
        if (state == null || state.isBootstrapCompleted() || userRepository.count() != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "First-owner setup has already been completed.");
        }
        String email = normalizeEmail(request.getEmail());
        ensurePasswordLength(request.getPassword());
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("Email is already in use.");

        Agency agency = agencyRepository.save(Agency.builder().name(request.getAgencyName().trim()).subscriptionPlan("PRO").build());
        AppUser owner = AppUser.builder().agency(agency).name(request.getOwnerName().trim()).email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword())).role(Role.OWNER).build();
        owner.setEmailVerified(false);
        userRepository.save(owner);
        state.setBootstrapCompleted(true);
        issueActionToken(owner, AccountTokenPurpose.EMAIL_VERIFICATION, LocalDateTime.now().plusHours(24),
                "/verify-email", "Verify your email address", "Verify your email within 24 hours to activate the owner account.");
    }

    @Transactional
    public void requestVerification(String inputEmail) {
        userRepository.findByEmail(normalizeEmail(inputEmail)).filter(user -> user.isEnabled() && !user.isEmailVerified())
                .ifPresent(user -> issueActionToken(user, AccountTokenPurpose.EMAIL_VERIFICATION,
                        LocalDateTime.now().plusHours(24), "/verify-email", "Verify your email address",
                        "Verify your email within 24 hours to activate your account."));
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AccountActionToken token = requireActionToken(rawToken, AccountTokenPurpose.EMAIL_VERIFICATION);
        AppUser user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        consume(token);
    }

    @Transactional
    public void requestPasswordReset(String inputEmail) {
        userRepository.findByEmail(normalizeEmail(inputEmail))
                .filter(user -> user.isEnabled() && user.isEmailVerified())
                .ifPresent(user -> issueActionToken(user, AccountTokenPurpose.PASSWORD_RESET,
                        LocalDateTime.now().plusHours(1), "/reset-password", "Reset your password",
                        "Use this link within one hour to choose a new password."));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        ensurePasswordLength(request.getPassword());
        AccountActionToken token = requireActionToken(request.getToken(), AccountTokenPurpose.PASSWORD_RESET);
        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        consume(token);
    }

    @Transactional
    public void invite(InviteMemberRequest request, UserPrincipal inviter) {
        if (request.getRole() != Role.ADMIN && request.getRole() != Role.MEMBER) {
            throw new IllegalArgumentException("Choose the Admin or Staff role.");
        }
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("This email already has an account.");
        invitationRepository.findFirstByAgencyIdAndEmailAndAcceptedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
                inviter.getAgencyId(), email).ifPresent(old -> old.setRevokedAt(LocalDateTime.now()));

        Agency agency = agencyRepository.findById(inviter.getAgencyId()).orElseThrow();
        AppUser inviterUser = userRepository.findById(inviter.getId()).orElseThrow();
        String rawToken = newOpaqueToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        invitationRepository.save(new AgencyInvitation(agency, inviterUser, email, request.getRole(),
                Sha256Util.hexDigest(rawToken), expiresAt, LocalDateTime.now()));
        emailDelivery.sendAccountLink(email, "Invitation to " + agency.getName(),
                "You were invited to join " + agency.getName() + " as " + displayRole(request.getRole())
                        + ". This invitation expires in 7 days.", emailDelivery.link("/accept-invite", rawToken));
    }

    @Transactional
    public void inviteClient(Long clientId, UserPrincipal inviter) {
        Client client = clientRepository.findByIdAndAgencyId(clientId, inviter.getAgencyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found."));
        String email = normalizeEmail(client.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email already has an account.");
        }
        invitationRepository.findFirstByAgencyIdAndEmailAndAcceptedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
                inviter.getAgencyId(), email).ifPresent(old -> old.setRevokedAt(LocalDateTime.now()));
        Agency agency = agencyRepository.findById(inviter.getAgencyId()).orElseThrow();
        AppUser inviterUser = userRepository.findById(inviter.getId()).orElseThrow();
        String rawToken = newOpaqueToken();
        invitationRepository.save(new AgencyInvitation(agency, inviterUser, email, Role.CLIENT,
                Sha256Util.hexDigest(rawToken), LocalDateTime.now().plusDays(7), LocalDateTime.now()));
        emailDelivery.sendAccountLink(email, "Client portal invitation to " + agency.getName(),
                "You were invited to access the client portal for " + agency.getName()
                        + ". This invitation expires in 7 days.", emailDelivery.link("/accept-invite", rawToken));
    }

    @Transactional
    public void acceptInvitation(AcceptInvitationRequest request) {
        ensurePasswordLength(request.getPassword());
        AgencyInvitation invite = invitationRepository.findByTokenHash(Sha256Util.hexDigest(request.getToken()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "Invitation is invalid or expired."));
        LocalDateTime now = LocalDateTime.now();
        if (invite.getAcceptedAt() != null || invite.getRevokedAt() != null || !now.isBefore(invite.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invitation is invalid or expired.");
        }
        if (userRepository.existsByEmail(invite.getEmail())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This email already has an account.");
        AppUser user = AppUser.builder().agency(invite.getAgency()).name(request.getName().trim()).email(invite.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())).role(invite.getRole()).build();
        user.setEmailVerified(true); // Possession of the unique invitation delivered to this address verifies it.
        userRepository.save(user);
        invite.setAcceptedAt(now);
    }

    @Transactional(readOnly = true)
    public TeamOverviewDto getTeam(Long agencyId) {
        List<TeamMemberDto> members = userRepository.findByAgencyId(agencyId).stream()
                .filter(user -> user.getRole() != Role.CLIENT)
                .map(user -> new TeamMemberDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isEnabled()))
                .toList();
        List<PendingInvitationDto> invitations = invitationRepository
                .findByAgencyIdAndAcceptedAtIsNullAndRevokedAtIsNull(agencyId).stream()
                .filter(invite -> invite.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(invite -> new PendingInvitationDto(invite.getId(), invite.getEmail(), invite.getRole(), invite.getExpiresAt()))
                .toList();
        return new TeamOverviewDto(members, invitations);
    }

    @Transactional
    public void revokeInvitation(Long invitationId, UserPrincipal owner) {
        AgencyInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));
        if (!invitation.getAgency().getId().equals(owner.getAgencyId())) throw new AccessDeniedException("Invitation belongs to another agency.");
        if (invitation.getAcceptedAt() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation has already been accepted.");
        invitation.setRevokedAt(LocalDateTime.now());
    }

    @Transactional
    public void deactivateMember(Long memberId, UserPrincipal owner) {
        if (memberId.equals(owner.getId())) throw new IllegalArgumentException("You cannot deactivate your own owner account.");
        AppUser user = userRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("Team member not found."));
        if (!user.getAgency().getId().equals(owner.getAgencyId()) || user.getRole() == Role.OWNER || user.getRole() == Role.CLIENT) {
            throw new AccessDeniedException("Only this agency's Admin and Staff accounts can be deactivated here.");
        }
        user.setEnabled(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    private void issueActionToken(AppUser user, AccountTokenPurpose purpose, LocalDateTime expiresAt,
                                  String path, String subject, String message) {
        AppUser issuanceUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        LocalDateTime now = LocalDateTime.now();
        boolean recentlyIssued = actionTokenRepository
                .findFirstByUser_IdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(issuanceUser.getId(), purpose)
                .map(token -> token.getCreatedAt().isAfter(now.minusSeconds(ACTION_EMAIL_COOLDOWN_SECONDS)))
                .orElse(false);
        if (recentlyIssued) return;

        actionTokenRepository.deleteByUserIdAndPurposeAndUsedAtIsNull(issuanceUser.getId(), purpose);
        String rawToken = newOpaqueToken();
        actionTokenRepository.save(new AccountActionToken(issuanceUser, Sha256Util.hexDigest(rawToken), purpose,
                expiresAt, now));
        emailDelivery.sendAccountLink(issuanceUser.getEmail(), subject, message, emailDelivery.link(path, rawToken));
    }

    private AccountActionToken requireActionToken(String rawToken, AccountTokenPurpose purpose) {
        AccountActionToken token = actionTokenRepository.findByTokenHashAndPurpose(Sha256Util.hexDigest(rawToken), purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "This link is invalid or has expired."));
        if (token.getUsedAt() != null || !LocalDateTime.now().isBefore(token.getExpiresAt()) || !token.getUser().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This link is invalid or has expired.");
        }
        return token;
    }

    private void consume(AccountActionToken token) { token.setUsedAt(LocalDateTime.now()); }

    private String newOpaqueToken() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private boolean constantTimeEquals(String expected, String supplied) {
        return supplied != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    private void ensurePasswordLength(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes.");
        }
    }

    private String displayRole(Role role) { return role == Role.MEMBER ? "Staff" : role.name(); }
}
