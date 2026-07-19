package com.freelancesuite.controller;

import com.freelancesuite.dto.AuthResponse;
import com.freelancesuite.dto.LoginRequest;
import com.freelancesuite.dto.RegisterRequest;
import com.freelancesuite.dto.UserDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserDto userDto = UserDto.builder()
                .id(userPrincipal.getId())
                .name(userPrincipal.getName())
                .email(userPrincipal.getEmail())
                .agencyId(userPrincipal.getAgencyId())
                .build();
        return ResponseEntity.ok(userDto);
    }
}
