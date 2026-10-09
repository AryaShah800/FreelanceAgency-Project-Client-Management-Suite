package com.freelancesuite.service;

import com.freelancesuite.dto.AcceptInvitationRequest;
import com.freelancesuite.dto.BootstrapOwnerRequest;
import com.freelancesuite.dto.InviteMemberRequest;
import com.freelancesuite.dto.LoginRequest;
import com.freelancesuite.dto.ResetPasswordRequest;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.AgencyInvitation;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.enums.DealStage;
import com.freelancesuite.entity.PlatformState;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.AccountActionTokenRepository;
import com.freelancesuite.repository.AgencyInvitationRepository;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.repository.ClientRepository;
import com.freelancesuite.repository.PlatformStateRepository;
import com.freelancesuite.security.JwtTokenProvider;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.util.Sha256Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class AccountLifecycleIntegrationTest {
    private static final String BOOTSTRAP_SECRET = "test-only-owner-bootstrap-secret-at-least-32-characters";
    private static final String INITIAL_PASSWORD = "OwnerPassword123!";

    @Autowired private AccountLifecycleService accountLifecycleService;
    @Autowired private AuthService authService;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private AppUserRepository userRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private PlatformStateRepository platformStateRepository;
    @Autowired private AccountActionTokenRepository actionTokenRepository;
    @Autowired private AgencyInvitationRepository invitationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private EmailDeliveryService emailDelivery;

    private final List<String> issuedTokens = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void captureIssuedTokensAndResetBootstrapState() {
        issuedTokens.clear();
        doAnswer(invocation -> {
            String path = invocation.getArgument(0);
            String token = invocation.getArgument(1);
            issuedTokens.add(token);
            return "https://app.example.test" + path + "?token=" + token;
        }).when(emailDelivery).link(anyString(), anyString());
        platformStateRepository.saveAndFlush(new PlatformState(1, false));
    }

    @Test
    @Transactional
    void firstOwnerMustVerifyEmailAndBootstrapIsOneTime() {
        BootstrapOwnerRequest request = new BootstrapOwnerRequest();
        request.setAgencyName("Launch Agency");
        request.setOwnerName("Launch Owner");
        request.setEmail(" OWNER@EXAMPLE.TEST ");
        request.setPassword(INITIAL_PASSWORD);
        request.setBootstrapSecret(BOOTSTRAP_SECRET);

        accountLifecycleService.bootstrapOwner(request);
        AppUser owner = userRepository.findByEmail("owner@example.test").orElseThrow();
        assertEquals(Role.OWNER, owner.getRole());
        assertFalse(owner.isEmailVerified());
        assertEquals(1, issuedTokens.size());

        assertThrows(DisabledException.class, () -> authService.login(login(owner.getEmail(), INITIAL_PASSWORD)));
        accountLifecycleService.verifyEmail(issuedTokens.get(0));
        assertTrue(userRepository.findById(owner.getId()).orElseThrow().isEmailVerified());
        assertNotNull(authService.login(login(owner.getEmail(), INITIAL_PASSWORD)).getToken());
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
                () -> accountLifecycleService.verifyEmail(issuedTokens.get(0))).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> accountLifecycleService.bootstrapOwner(request)).getStatusCode());
    }

    @Test
    void repeatedBadPasswordsAreThrottledAndDoNotAllowTheCorrectPasswordDuringLockout() {
        String email = "lockout-" + UUID.randomUUID() + "@example.test";
        createVerifiedOwner(email);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(BadCredentialsException.class,
                    () -> authService.login(login(email, "WrongPassword123!")));
        }

        ResponseStatusException locked = assertThrows(ResponseStatusException.class,
                () -> authService.login(login(email, INITIAL_PASSWORD)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatusCode());
    }

    @Test
    @Transactional
    void passwordResetChangesPasswordInvalidatesOldJwtAndConsumesToken() {
        AppUser owner = createVerifiedOwner("reset@example.test");
        String oldJwt = authService.login(login(owner.getEmail(), INITIAL_PASSWORD)).getToken();
        assertEquals(0, jwtTokenProvider.getTokenVersionFromJWT(oldJwt));

        accountLifecycleService.requestPasswordReset(owner.getEmail().toUpperCase());
        accountLifecycleService.requestPasswordReset(owner.getEmail());
        assertEquals(1, issuedTokens.size());
        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setToken(issuedTokens.get(0));
        reset.setPassword("ChangedPassword456!");
        accountLifecycleService.resetPassword(reset);

        AppUser updated = userRepository.findById(owner.getId()).orElseThrow();
        assertEquals(1, updated.getTokenVersion());
        assertNotEquals(updated.getTokenVersion(), jwtTokenProvider.getTokenVersionFromJWT(oldJwt));
        assertNotNull(authService.login(login(owner.getEmail(), "ChangedPassword456!")).getToken());
        assertThrows(BadCredentialsException.class, () -> authService.login(login(owner.getEmail(), INITIAL_PASSWORD)));
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
                () -> accountLifecycleService.resetPassword(reset)).getStatusCode());
        assertTrue(actionTokenRepository.findAll().stream().anyMatch(token -> token.getUsedAt() != null));
    }

    @Test
    void simultaneousPasswordResetRequestsIssueOnlyOneEmail() throws Exception {
        AppUser owner = createVerifiedOwner("concurrent-" + UUID.randomUUID() + "@example.test");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable request = () -> {
            ready.countDown();
            try {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test start timed out.");
                accountLifecycleService.requestPasswordReset(owner.getEmail());
            } catch (Throwable ex) {
                failure.compareAndSet(null, ex);
            }
        };

        Thread first = new Thread(request, "password-reset-request-1");
        Thread second = new Thread(request, "password-reset-request-2");
        first.start();
        second.start();
        assertTrue(ready.await(5, TimeUnit.SECONDS), "Both requests should be ready to run.");
        start.countDown();
        first.join(10_000);
        second.join(10_000);

        assertFalse(first.isAlive(), "First request should finish.");
        assertFalse(second.isAlive(), "Second request should finish.");
        assertNull(failure.get(), "Password reset requests should complete without errors.");
        assertEquals(1, issuedTokens.size(), "Only one reset link should be issued inside the cooldown window.");
    }

    @Test
    @Transactional
    void invitationsAreSingleUseAndCannotGrantOwnerRole() {
        AppUser owner = createVerifiedOwner("inviter@example.test");
        InviteMemberRequest ownerRoleRequest = inviteRequest("blocked@example.test", Role.OWNER);
        assertThrows(IllegalArgumentException.class,
                () -> accountLifecycleService.invite(ownerRoleRequest, UserPrincipal.create(owner)));
        assertTrue(issuedTokens.isEmpty());

        accountLifecycleService.invite(inviteRequest("staff@example.test", Role.MEMBER), UserPrincipal.create(owner));
        String invitationToken = issuedTokens.get(0);
        AgencyInvitation invitation = invitationRepository.findByTokenHash(Sha256Util.hexDigest(invitationToken)).orElseThrow();
        assertEquals(Role.MEMBER, invitation.getRole());
        assertEquals("staff@example.test", invitation.getEmail());

        AcceptInvitationRequest accept = new AcceptInvitationRequest();
        accept.setToken(invitationToken);
        accept.setName("Staff Member");
        accept.setPassword("StaffPassword123!");
        accountLifecycleService.acceptInvitation(accept);

        AppUser staff = userRepository.findByEmail("staff@example.test").orElseThrow();
        assertEquals(Role.MEMBER, staff.getRole());
        assertTrue(staff.isEmailVerified());
        assertNotNull(invitation.getAcceptedAt());
        assertNotNull(authService.login(login(staff.getEmail(), "StaffPassword123!")).getToken());
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
                () -> accountLifecycleService.acceptInvitation(accept)).getStatusCode());
    }

    @Test
    @Transactional
    void clientPortalInvitationIsBoundToAnAgencyClientAndCreatesOnlyClientRole() {
        AppUser owner = createVerifiedOwner("client-inviter@example.test");
        Client client = clientRepository.saveAndFlush(Client.builder()
                .agency(owner.getAgency()).companyName("Client Portal Co").contactPerson("Client Person")
                .email("portal-client@example.test").dealStage(DealStage.WON).build());

        accountLifecycleService.inviteClient(client.getId(), UserPrincipal.create(owner));
        String token = issuedTokens.get(0);
        AgencyInvitation invitation = invitationRepository.findByTokenHash(Sha256Util.hexDigest(token)).orElseThrow();
        assertEquals(Role.CLIENT, invitation.getRole());
        assertEquals(client.getEmail(), invitation.getEmail());

        AcceptInvitationRequest accept = new AcceptInvitationRequest();
        accept.setToken(token);
        accept.setName("Portal Client");
        accept.setPassword("ClientPassword123!");
        accountLifecycleService.acceptInvitation(accept);
        AppUser created = userRepository.findByEmail(client.getEmail()).orElseThrow();
        assertEquals(Role.CLIENT, created.getRole());
        assertTrue(created.isEmailVerified());
        assertEquals(owner.getAgency().getId(), created.getAgency().getId());

        Agency foreignAgency = agencyRepository.saveAndFlush(Agency.builder().name("Foreign Agency").build());
        Client foreignClient = clientRepository.saveAndFlush(Client.builder().agency(foreignAgency)
                .companyName("Foreign Client").email("foreign-client@example.test").dealStage(DealStage.LEAD).build());
        int invitationCount = issuedTokens.size();
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> accountLifecycleService.inviteClient(foreignClient.getId(), UserPrincipal.create(owner)))
                .getStatusCode());
        assertEquals(invitationCount, issuedTokens.size(), "A foreign client must not trigger an invitation email.");
    }

    private AppUser createVerifiedOwner(String email) {
        Agency agency = agencyRepository.saveAndFlush(Agency.builder().name("Lifecycle Test Agency").build());
        AppUser owner = AppUser.builder().agency(agency).name("Test Owner").email(email)
                .passwordHash(passwordEncoder.encode(INITIAL_PASSWORD)).role(Role.OWNER).build();
        owner.setEmailVerified(true);
        return userRepository.saveAndFlush(owner);
    }

    private InviteMemberRequest inviteRequest(String email, Role role) {
        InviteMemberRequest request = new InviteMemberRequest();
        request.setEmail(email);
        request.setRole(role);
        return request;
    }

    private LoginRequest login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
