package com.taskflow.taskflow.controller;

import com.taskflow.taskflow.dto.ApiResponse;
import com.taskflow.taskflow.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints for user authentication")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    @Operation(
            summary = "User Login",
            description = "Authenticates a user and returns a JWT token upon successful login"
    )
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest req) {
        log.info("Login request received for email={}", req == null ? null : req.email());
        try {
            assert req != null;
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password())
            );

            String token = tokenProvider.createToken(auth.getName());
            log.info("Authentication successful for email={}", req.email());
            return ResponseEntity.ok(ApiResponse.ok("Authenticated", Map.of("token", token)));
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for email={}: {}", req.email(), ex.getMessage());
            throw new BadCredentialsException("Invalid email/password");
        }
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }
}
