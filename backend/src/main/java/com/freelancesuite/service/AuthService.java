package com.freelancesuite.service;

import com.freelancesuite.dto.AuthResponse;
import com.freelancesuite.dto.LoginRequest;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.security.JwtTokenProvider;
import com.freelancesuite.security.UserPrincipal;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AgencyRepository agencyRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    // Keep account-level lockout state bounded when attackers submit random emails.
    private final Cache<String, FailedLoginState> failedLogins = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_BLOCK_MS = 15 * 60 * 1000; // 15 minutes

    @Autowired
    public AuthService(AgencyRepository agencyRepository, AppUserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse register(com.freelancesuite.dto.RegisterRequest ignored) {
        throw new ResponseStatusException(HttpStatus.GONE, "Public registration is closed. Ask an agency owner for an invitation.");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        request.setEmail(email);
        long now = System.currentTimeMillis();

        FailedLoginState failedLogin = failedLogins.getIfPresent(email);
        if (failedLogin != null && now - failedLogin.lastFailureAtMillis() < LOGIN_BLOCK_MS) {
            if (failedLogin.attempts() >= MAX_LOGIN_ATTEMPTS) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Account locked due to too many failed attempts. Try again in 15 minutes.");
            }
        } else if (failedLogin != null) {
            failedLogins.invalidate(email);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            // Success - reset attempts
            failedLogins.invalidate(email);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            failedLogins.asMap().compute(email, (key, current) -> {
                if (current == null || now - current.lastFailureAtMillis() >= LOGIN_BLOCK_MS) {
                    return new FailedLoginState(1, now);
                }
                return new FailedLoginState(current.attempts() + 1, now);
            });
            throw ex;
        }

        String token = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        AppUser user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .agencyId(user.getAgency().getId())
                .agencyName(user.getAgency().getName())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record FailedLoginState(int attempts, long lastFailureAtMillis) { }
}
