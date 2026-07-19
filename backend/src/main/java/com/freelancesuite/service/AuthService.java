package com.freelancesuite.service;

import com.freelancesuite.dto.AuthResponse;
import com.freelancesuite.dto.LoginRequest;
import com.freelancesuite.dto.RegisterRequest;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.AppUser;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.AppUserRepository;
import com.freelancesuite.security.JwtTokenProvider;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AgencyRepository agencyRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Autowired
    public AuthService(AgencyRepository agencyRepository, AppUserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use!");
        }

        Agency agency = Agency.builder()
                .name(request.getAgencyName())
                .gstin(request.getGstin())
                .subscriptionPlan("PRO")
                .build();
        agency = agencyRepository.save(agency);

        AppUser owner = AppUser.builder()
                .agency(agency)
                .name(request.getOwnerName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.OWNER)
                .hourlyRate(100.0)
                .build();
        owner = userRepository.save(owner);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .userId(owner.getId())
                .name(owner.getName())
                .email(owner.getEmail())
                .role(owner.getRole().name())
                .agencyId(agency.getId())
                .agencyName(agency.getName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

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
}
