# Phase 2: Email/Password Authentication

## Overview

Phase 2 implements the traditional email/password authentication flow. This includes user registration, login, and JWT token generation for authenticated users.

## Tasks

### 1. Create Request DTOs

#### Authentication Request DTO

**File**: `src/main/java/me/boonyarit/finance/dto/request/AuthenticationRequest.java`

```java
package me.boonyarit.finance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthenticationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

#### Register Request DTO

**File**: `src/main/java/me/boonyarit/finance/dto/request/RegisterRequest.java`

```java
package me.boonyarit.finance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
```

### 2. Create Response DTO

#### Authentication Response DTO

**File**: `src/main/java/me/boonyarit/finance/dto/response/AuthenticationResponse.java`

```java
package me.boonyarit.finance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticationResponse {
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String provider;
}
```

### 3. Create Custom Exception

#### Authentication Exception

**File**: `src/main/java/me/boonyarit/finance/exception/AuthenticationException.java`

```java
package me.boonyarit.finance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### User Already Exists Exception

**File**: `src/main/java/me/boonyarit/finance/exception/UserAlreadyExistsException.java`

```java
package me.boonyarit.finance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
```

### 4. Create Authentication Service

#### Authentication Service Implementation

**File**: `src/main/java/me/boonyarit/finance/service/AuthenticationService.java`

```java
package me.boonyarit.finance.service;

import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.dto.request.AuthenticationRequest;
import me.boonyarit.finance.dto.request.RegisterRequest;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.enumeration.AuthProvider;
import me.boonyarit.finance.enumeration.Role;
import me.boonyarit.finance.exception.AuthenticationException;
import me.boonyarit.finance.exception.UserAlreadyExistsException;
import me.boonyarit.finance.security.JwtService;
import me.boonyarit.finance.entity.UserEntity;
import me.boonyarit.finance.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Create new user
        UserEntity user = UserEntity.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)  // Default role
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);

        // Generate JWT token
        String token = jwtService.generateToken(user, AuthProvider.LOCAL);

        return AuthenticationResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .provider(user.getProvider().name())
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserEntity user = (UserEntity) authentication.getPrincipal();

            // Generate JWT token
            String token = jwtService.generateToken(user, user.getProvider());

            return AuthenticationResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .provider(user.getProvider().name())
                    .build();

        } catch (UsernameNotFoundException e) {
            throw new AuthenticationException("Invalid email or password");
        } catch (Exception e) {
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        }
    }
}
```

### 5. Create Authentication Controller

#### AuthController Implementation

**File**: `src/main/java/me/boonyarit/finance/controller/AuthController.java`

```java
package me.boonyarit.finance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.dto.request.AuthenticationRequest;
import me.boonyarit.finance.dto.request.RegisterRequest;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class AuthController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // For JWT, logout is handled client-side by removing the token
        // This endpoint exists for completeness and future extensions
        return ResponseEntity.ok().build();
    }
}
```

### 6. Create Global Exception Handler

#### GlobalExceptionHandler

**File**: `src/main/java/me/boonyarit/finance/exception/GlobalExceptionHandler.java`

```java
package me.boonyarit.finance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("error", "Unauthorized");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Error");

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        response.put("message", "Validation failed");
        response.put("fieldErrors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("error", "Unauthorized");
        response.put("message", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
```

### 7. Update Security Config

#### Allow Auth Endpoints

Update the `securityFilterChain` method in `SecurityConfig.java` to include the new endpoint:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/error").permitAll()  // For Spring Boot error handling
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

## Testing the Implementation

### 1. Test User Registration

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "password": "SecurePass123"
  }'
```

Expected Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "provider": "LOCAL"
}
```

### 2. Test User Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123"
  }'
```

### 3. Test Protected Endpoint

```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Unit Tests

### Authentication Service Test

**File**: `src/test/java/me/boonyarit/finance/service/AuthenticationServiceTest.java`

```java
package me.boonyarit.finance.service;

import me.boonyarit.finance.dto.request.AuthenticationRequest;
import me.boonyarit.finance.dto.request.RegisterRequest;
import me.boonyarit.finance.enumeration.AuthProvider;
import me.boonyarit.finance.enumeration.Role;
import me.boonyarit.finance.exception.UserAlreadyExistsException;
import me.boonyarit.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthenticationService(
                userRepository,
                passwordEncoder,
                jwtService,
                authenticationManager
        );
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void register_ShouldCreateUser_WhenEmailIsUnique() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPassword("password");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(), any())).thenReturn("jwtToken");

        // When
        var response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("john@example.com", response.getEmail());
        assertEquals("jwtToken", response.getToken());
        verify(userRepository).save(any());
    }
}
```

## Implementation Checklist

### Before Moving to Phase 3

- [ ] Create all DTO classes (requests and responses)
- [ ] Implement AuthenticationService with register and authenticate methods
- [ ] Create AuthController with REST endpoints
- [ ] Add global exception handling
- [ ] Update SecurityConfig to allow auth endpoints
- [ ] Test registration and login flows
- [ ] Create unit tests for authentication logic
- [ ] Verify JWT tokens are properly generated and validated

## Common Issues

### Validation Errors

Ensure DTOs have proper validation annotations:

- `@NotBlank` for required fields
- `@Email` for email validation
- `@Size` for password length constraints

### CORS Issues

If testing from a frontend application, ensure CORS is properly configured:

```java
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
```

### JWT Token Expiration

Remember that tokens expire based on the configuration in `application.yaml`:

```yaml
app:
  jwt:
    expiration: 900000 # 15 minutes in milliseconds
```

## Next Phase

Phase 3 will implement Google OAuth2 authentication, allowing users to:

- Authenticate with their Google account
- Link Google accounts to existing email-based accounts
- Switch between authentication providers

## Time Estimate

Phase 2 should take approximately 3-4 hours to complete:

- 45 minutes: DTOs and exceptions
- 60 minutes: AuthenticationService implementation
- 45 minutes: AuthController setup
- 30 minutes: Exception handling
- 60 minutes: Testing and validation
