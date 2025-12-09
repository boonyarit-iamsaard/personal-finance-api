# Phase 3: Google OAuth Integration

## Overview

Phase 3 adds Google OAuth2 authentication to the system. This allows users to authenticate using their Google account, with automatic user creation and linking to existing accounts.

## Prerequisites

Before starting Phase 3, ensure you have:

- Google Cloud Console project set up
- OAuth2 credentials (Client ID and Client Secret)
- Authorized redirect URI configured

## Tasks

### 1. Update Application Configuration

#### application.yaml

Add OAuth2 client configuration:

```yaml
spring:
  application:
    name: Finance
  datasource:
    url: jdbc:postgresql://localhost:5432/personal-finance
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    show-sql: true

  # OAuth2 Configuration
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
            redirect-uri: "{baseUrl}/api/auth/oauth/callback/google"
            client-name: Google
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v2/userinfo
            user-name-attribute: email

# JWT Configuration
app:
  jwt:
    secret: ${JWT_SECRET:mySecretKey123456789012345678901234567890}
    expiration: 900000  # 15 minutes in milliseconds

# CORS Configuration
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

### 2. Create OAuth2 User DTO

#### OAuth2UserResponse

**File**: `src/main/java/me/boonyarit/finance/dto/OAuth2UserResponse.java`

```java
package me.boonyarit.finance.dto;

import lombok.Data;

import java.util.Map;

@Data
public class OAuth2UserResponse {
    private String id;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    private String picture;
    private String locale;

    public static OAuth2UserResponse fromGoogleAttributes(Map<String, Object> attributes) {
        OAuth2UserResponse response = new OAuth2UserResponse();
        response.setId((String) attributes.get("id"));
        response.setEmail((String) attributes.get("email"));
        response.setName((String) attributes.get("name"));
        response.setGivenName((String) attributes.get("given_name"));
        response.setFamilyName((String) attributes.get("family_name"));
        response.setPicture((String) attributes.get("picture"));
        response.setLocale((String) attributes.get("locale"));
        return response;
    }
}
```

### 3. Create OAuth2 Service

#### OAuth2Service Implementation

**File**: `src/main/java/me/boonyarit/finance/service/OAuth2Service.java`

```java
package me.boonyarit.finance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.boonyarit.finance.dto.OAuth2UserResponse;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.enumeration.AuthProvider;
import me.boonyarit.finance.enumeration.Role;
import me.boonyarit.finance.repository.UserRepository;
import me.boonyarit.finance.entity.UserEntity;
import me.boonyarit.finance.security.JwtService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthenticationResponse processOAuth2User(OAuth2User oAuth2User) {
        // Extract user information from Google
        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserResponse userInfo = OAuth2UserResponse.fromGoogleAttributes(attributes);

        Optional<UserEntity> userOptional = userRepository.findByEmail(userInfo.getEmail());
        UserEntity user;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            // Update Google info if user exists
            if (user.getProvider() != AuthProvider.GOOGLE) {
                // User exists with email/password, linking to Google
                user.setProvider(AuthProvider.GOOGLE);
                log.info("Linking existing user account {} with Google OAuth", user.getEmail());
            }
        } else {
            // Create new user from Google OAuth
            user = UserEntity.builder()
                    .email(userInfo.getEmail())
                    .firstName(userInfo.getGivenName())
                    .lastName(userInfo.getFamilyName())
                    .role(Role.USER)
                    .provider(AuthProvider.GOOGLE)
                    .build();

            log.info("Creating new user from Google OAuth: {}", user.getEmail());
        }

        userRepository.save(user);

        // Generate JWT token
        String token = jwtService.generateToken(user, AuthProvider.GOOGLE);

        return AuthenticationResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .provider(user.getProvider().name())
                .build();
    }
}
```

### 4. Create OAuth2 Success Handler

#### OAuth2AuthenticationSuccessHandler

**File**: `src/main/java/me/boonyarit/finance/security/OAuth2AuthenticationSuccessHandler.java`

```java
package me.boonyarit.finance.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.service.OAuth2Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2Service oAuth2Service;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        try {
            AuthenticationResponse authResponse = oAuth2Service.processOAuth2User(oAuth2User);

            // Check if request is from API (Accept: application/json)
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                // Return JSON response for API calls
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(authResponse));
            } else {
                // Redirect for web application with token as parameter
                String token = URLEncoder.encode(authResponse.getToken(), StandardCharsets.UTF_8);
                String redirectUrl = String.format("%s?token=%s", determineTargetUrl(request, response), token);
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            }
        } catch (Exception e) {
            log.error("Error processing OAuth2 authentication", e);
            throw new ServletException("Error processing OAuth2 authentication", e);
        }
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        // For development, redirect to frontend URL
        // In production, this should be configurable
        String redirectUrl = request.getParameter("redirect_uri");
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            redirectUrl = "http://localhost:3000/auth/callback";
        }
        return redirectUrl;
    }
}
```

### 5. Create OAuth2 Failure Handler

#### OAuth2AuthenticationFailureHandler

**File**: `src/main/java/me/boonyarit/finance/security/OAuth2AuthenticationFailureHandler.java`

```java
package me.boonyarit.finance.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 authentication failed", exception);

        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);

        // Check if request is from API
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            // Return JSON error response
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(String.format(
                "{\"error\": {\"code\": \"OAUTH2_ERROR\", \"message\": \"%s\"}}",
                exception.getMessage()
            ));
        } else {
            // Redirect with error for web application
            String redirectUrl = String.format("%s?error=%s",
                getDefaultFailureUrl(), errorMessage);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
}
```

### 6. Update Security Configuration

#### Add OAuth2 Configuration to SecurityConfig

Update `SecurityConfig.java` to include OAuth2 configuration:

```java
package me.boonyarit.finance.config;

import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.security.JwtAuthenticationFilter;
import me.boonyarit.finance.security.OAuth2AuthenticationFailureHandler;
import me.boonyarit.finance.security.OAuth2AuthenticationSuccessHandler;
import me.boonyarit.finance.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserService userService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/auth/oauth/**").permitAll()  // OAuth endpoints
                .requestMatchers("/oauth2/**", "/login/**").permitAll()  // Spring OAuth2 endpoints
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/api/auth/oauth/authorize")
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/api/auth/oauth/callback/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService())
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.client.userinfo.OAuth2UserService<org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest, org.springframework.security.oauth2.core.user.OAuth2User> oauth2UserService() {
        return new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 7. Add OAuth2 Controller Endpoints

#### Update AuthController

Add OAuth2 authorization endpoint to `AuthController.java`:

```java
package me.boonyarit.finance.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.dto.request.AuthenticationRequest;
import me.boonyarit.finance.dto.request.RegisterRequest;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class AuthController {

    private final AuthenticationService authService;
    private final ClientRegistrationRepository clientRegistrationRepository;

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
        return ResponseEntity.ok().build();
    }

    @GetMapping("/oauth/google/url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl(HttpServletRequest request) {
        ClientRegistration googleRegistration = clientRegistrationRepository.findByRegistrationId("google");

        String baseUrl = request.getScheme() + "://" + request.getServerName() +
                        (request.getServerPort() != 80 && request.getServerPort() != 443 ? ":" + request.getServerPort() : "");

        String redirectUri = baseUrl + "/api/auth/oauth/callback/google";
        String authUrl = String.format(
            "https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
            googleRegistration.getClientId(),
            redirectUri,
            String.join(" ", googleRegistration.getScopes())
        );

        Map<String, String> response = new HashMap<>();
        response.put("url", authUrl);
        response.put("redirectUri", redirectUri);

        return ResponseEntity.ok(response);
    }
}
```

## Testing OAuth2 Implementation

### 1. Get Google Authorization URL

```bash
curl -X GET http://localhost:8080/api/auth/oauth/google/url
```

### 2. Manual OAuth2 Flow

1. Visit the authorization URL returned from the endpoint above
2. Sign in with your Google account
3. After authorization, you'll be redirected to your callback URL with a JWT token

### 3. API-Based OAuth2 Flow (for mobile/single-page apps)

```bash
# Start OAuth2 flow
curl -X GET http://localhost:8080/oauth2/authorization/google

# After Google redirects back, you'll receive the token in the response
```

## Implementation Checklist

### Before Moving to Phase 4

- [ ] Configure OAuth2 in application.yaml
- [ ] Create OAuth2UserResponse DTO
- [ ] Implement OAuth2Service for processing Google users
- [ ] Create OAuth2 success and failure handlers
- [ ] Update SecurityConfig with OAuth2 configuration
- [ ] Add OAuth2 authorization endpoint to AuthController
- [ ] Test OAuth2 flow with Google
- [ ] Verify token generation for OAuth users
- [ ] Test account linking (existing email + OAuth)

## Common Issues

### Redirect URI Mismatch

Ensure the redirect URI in Google Cloud Console matches exactly:

- Local: `http://localhost:8080/api/auth/oauth/callback/google`
- Production: `https://your-domain.com/api/auth/oauth/callback/google`

### CORS Issues

OAuth2 redirects might encounter CORS issues. Update your CORS configuration as needed.

### Token Validation

OAuth2-generated tokens should be validated the same way as email/password tokens using the JWT filter.

## Next Phase

Phase 4 will enhance the authentication system with:

- Refresh tokens for better user experience
- Current user annotation for accessing authenticated user
- CORS configuration for frontend integration
- Rate limiting for security

## Time Estimate

Phase 3 should take approximately 4-5 hours to complete:

- 45 minutes: OAuth2 configuration
- 60 minutes: OAuth2Service implementation
- 60 minutes: Success/failure handlers
- 45 minutes: SecurityConfig updates
- 45 minutes: Controller updates
- 60 minutes: Testing and debugging
- 45 minutes: Documentation and validation
