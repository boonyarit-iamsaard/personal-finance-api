package me.boonyarit.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.annotation.CurrentUser;
import me.boonyarit.finance.dto.request.AuthenticationRequest;
import me.boonyarit.finance.dto.request.LogoutRequest;
import me.boonyarit.finance.dto.request.RefreshTokenRequest;
import me.boonyarit.finance.dto.request.RegisterRequest;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.dto.response.UserResponse;
import me.boonyarit.finance.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account and returns authentication tokens."
    )
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "409", description = "User already exists")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates user credentials and returns authentication tokens."
    )
    @ApiResponse(responseCode = "200", description = "User authenticated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    public ResponseEntity<AuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    @Operation(
        summary = "Refresh authentication token",
        description = "Generates a new authentication token using a valid refresh token."
    )
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthenticationResponse response = authenticationService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Invalidates the provided refresh token to log out the user."
    )
    @ApiResponse(responseCode = "204", description = "User logged out successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user",
        description = "Retrieves information about the currently authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "User retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser UserDetails currentUser) {
        UserResponse response = authenticationService.getCurrentUser(currentUser);
        return ResponseEntity.ok(response);
    }
}
