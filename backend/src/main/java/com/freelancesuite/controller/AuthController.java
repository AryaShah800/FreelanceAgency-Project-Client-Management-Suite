package com.freelancesuite.controller;

import com.freelancesuite.dto.AuthResponse;
import com.freelancesuite.dto.LoginRequest;
import com.freelancesuite.dto.UserDto;
import com.freelancesuite.dto.*;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.AuthService;
import com.freelancesuite.service.AccountLifecycleService;
import com.freelancesuite.repository.AgencyRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AgencyRepository agencyRepository;
    private final AccountLifecycleService accountLifecycleService;

    @Autowired
    public AuthController(AuthService authService, AgencyRepository agencyRepository,
                          AccountLifecycleService accountLifecycleService) {
        this.authService = authService;
        this.agencyRepository = agencyRepository;
        this.accountLifecycleService = accountLifecycleService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request,
                                                         HttpServletRequest httpRequest) {
        accountLifecycleService.registerAgencyOwner(request, httpRequest.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of("message", "Account created. Check your email to verify your address before signing in."));
    }

    @PostMapping("/bootstrap-owner")
    public ResponseEntity<Map<String, String>> bootstrapOwner(@Valid @RequestBody BootstrapOwnerRequest request) {
        accountLifecycleService.bootstrapOwner(request);
        return ResponseEntity.accepted().body(Map.of("message", "Check your email to verify the owner account."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody EmailTokenRequest request) {
        accountLifecycleService.verifyEmail(request.getToken());
        return ResponseEntity.ok(Map.of("message", "Email verified. You can sign in now."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody EmailAddressRequest request) {
        accountLifecycleService.requestVerification(request.getEmail());
        return ResponseEntity.accepted().body(Map.of("message", "If the account needs verification, an email has been sent."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody EmailAddressRequest request) {
        accountLifecycleService.requestPasswordReset(request.getEmail());
        return ResponseEntity.accepted().body(Map.of("message", "If the account exists, a reset email has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountLifecycleService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password changed. Sign in with your new password."));
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<Map<String, String>> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        accountLifecycleService.acceptInvitation(request);
        return ResponseEntity.ok(Map.of("message", "Invitation accepted. You can sign in now."));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserDto userDto = UserDto.builder()
                .id(userPrincipal.getId())
                .name(userPrincipal.getName())
                .email(userPrincipal.getEmail())
                .role(userPrincipal.getRole())
                .agencyId(userPrincipal.getAgencyId())
                .agencyName(agencyRepository.findById(userPrincipal.getAgencyId())
                        .map(com.freelancesuite.entity.Agency::getName).orElse("Agency"))
                .build();
        return ResponseEntity.ok(userDto);
    }
}
