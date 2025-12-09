# Phase 4: Authentication Enhancements

## Overview

Phase 4 adds advanced features to improve user experience and security. This includes refresh tokens, convenient access to authenticated users, CORS configuration, and rate limiting.

## Tasks

### 1. Create Refresh Token Entity

#### RefreshTokenEntity

**File**: `src/main/java/me/boonyarit/finance/entity/RefreshTokenEntity.java`

```java
package me.boonyarit.finance.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
public class RefreshTokenEntity extends BaseEntity {

    private String token;

    @ManyToOne
    private UserEntity user;

    private Instant expiryDate;

    private boolean revoked;

    public RefreshTokenEntity(String token, UserEntity user, Instant expiryDate) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
        this.revoked = false;
    }
}
```

### 2. Create Refresh Token Repository

#### RefreshTokenRepository

**File**: `src/main/java/me/boonyarit/finance/repository/RefreshTokenRepository.java`

```java
package me.boonyarit.finance.repository;

import me.boonyarit.finance.entity.RefreshTokenEntity;
import me.boonyarit.finance.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    List<RefreshTokenEntity> findByUser(UserEntity user);

    List<RefreshTokenEntity> findAllByExpiryDateBeforeAndRevoked(Instant date, boolean revoked);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.user = :user")
    void revokeAllUserTokens(UserEntity user);
}
```

### 3. Create Refresh Token Service

#### RefreshTokenService

**File**: `src/main/java/me/boonyarit/finance/service/RefreshTokenService.java`

```java
package me.boonyarit.finance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.boonyarit.finance.entity.RefreshTokenEntity;
import me.boonyarit.finance.entity.UserEntity;
import me.boonyarit.finance.exception.RefreshTokenException;
import me.boonyarit.finance.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${app.jwt.refresh-expiration:604800000}")  // 7 days
    private Long refreshExpiration;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenEntity createRefreshToken(UserEntity user) {
        // Revoke all existing tokens for the user
        revokeAllUserTokens(user);

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build();

        refreshToken = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user: {}", user.getEmail());
        return refreshToken;
    }

    public RefreshTokenEntity findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));
    }

    public RefreshTokenEntity verifyExpiration(RefreshTokenEntity token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException("Refresh token was expired. Please make a new login request");
        }
        return token;
    }

    @Transactional
    public RefreshTokenEntity refreshToken(String requestRefreshToken) {
        RefreshTokenEntity token = findByToken(requestRefreshToken);
        RefreshTokenEntity verifiedToken = verifyExpiration(token);

        // Revoke current token and create a new one
        revokeToken(verifiedToken);
        return createRefreshToken(verifiedToken.getUser());
    }

    @Transactional
    public void revokeToken(RefreshTokenEntity token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAllUserTokens(UserEntity user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.findAllByExpiryDateBeforeAndRevoked(Instant.now(), false)
                .forEach(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }
}
```

### 4. Create CurrentUser Annotation

#### CurrentUser Annotation

**File**: `src/main/java/me/boonyarit/finance/annotation/CurrentUser.java`

```java
package me.boonyarit.finance.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
```

#### CurrentUser Argument Resolver

**File**: `src/main/java/me/boonyarit/finance/resolver/CurrentUserArgumentResolver.java`

```java
package me.boonyarit.finance.resolver;

import me.boonyarit.finance.annotation.CurrentUser;
import me.boonyarit.finance.entity.UserEntity;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentUser.class) != null &&
               parameter.getParameterType().equals(UserEntity.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserEntity) {
            return authentication.getPrincipal();
        }

        throw new IllegalArgumentException("User not authenticated");
    }
}
```

### 5. Update Authentication Service

#### Add Refresh Token Support

Update `AuthenticationService.java` to include refresh token functionality:

```java
// Add these fields and methods to the existing AuthenticationService

private final RefreshTokenService refreshTokenService;

// Update the authenticate method to return refresh token
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

        // Create refresh token
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
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

// Add refresh token method
public AuthenticationResponse refreshToken(String refreshToken) {
    RefreshTokenEntity token = refreshTokenService.refreshToken(refreshToken);
    UserEntity user = token.getUser();

    String newToken = jwtService.generateToken(user, user.getProvider());

    return AuthenticationResponse.builder()
            .token(newToken)
            .refreshToken(token.getToken())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .provider(user.getProvider().name())
            .build();
}

// Add logout method
public void logout(String refreshToken) {
    refreshTokenRepository.findByToken(refreshToken)
            .ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
}
```

### 6. Update DTOs

#### Add Refresh Token to AuthenticationResponse

Update `AuthenticationResponse.java`:

```java
@Data
@Builder
public class AuthenticationResponse {
    private String token;
    private String refreshToken;  // Add this field
    private String email;
    private String firstName;
    private String lastName;
    private String provider;
}
```

#### Create RefreshTokenRequest

**File**: `src/main/java/me/boonyarit/finance/dto/request/RefreshTokenRequest.java`

```java
package me.boonyarit.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
```

### 7. Update Controllers

#### Add Refresh Token Endpoints to AuthController

Update `AuthController.java`:

```java
// Add these new endpoints

@PostMapping("/refresh-token")
public ResponseEntity<AuthenticationResponse> refreshToken(
        @Valid @RequestBody RefreshTokenRequest request) {
    AuthenticationResponse response = authService.refreshToken(request.getRefreshToken());
    return ResponseEntity.ok(response);
}

@PostMapping("/logout")
public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
    authService.logout(request.getRefreshToken());
    return ResponseEntity.ok().build();
}

// Add endpoint to get current user info
@GetMapping("/me")
public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser UserEntity user) {
    UserResponse response = UserResponse.builder()
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .provider(user.getProvider().name())
            .role(user.getRole().name())
            .build();
    return ResponseEntity.ok(response);
}
```

#### Create UserResponse DTO

**File**: `src/main/java/me/boonyarit/finance/dto/response/UserResponse.java`

```java
package me.boonyarit.finance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String email;
    private String firstName;
    private String lastName;
    private String provider;
    private String role;
}
```

### 8. Create Rate Limiting Filter

#### RateLimitingFilter

**File**: `src/main/java/me/boonyarit/finance/filter/RateLimitingFilter.java`

```java
package me.boonyarit.finance.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter implements Filter {

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastResetTime = new ConcurrentHashMap<>();

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);

        // Skip rate limiting for non-auth endpoints
        if (!httpRequest.getRequestURI().startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastReset = lastResetTime.getOrDefault(clientIp, 0L);

        // Reset counter if more than a minute has passed
        if (currentTime - lastReset > 60000) {
            requestCounts.put(clientIp, new AtomicInteger(0));
            lastResetTime.put(clientIp, currentTime);
        }

        AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));

        if (count.incrementAndGet() > requestsPerMinute) {
            httpResponse.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            httpResponse.getWriter().write("Rate limit exceeded");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 9. Update Web Configuration

#### WebConfig for Argument Resolver and Filters

**File**: `src/main/java/me/boonyarit/finance/config/WebConfig.java`

```java
package me.boonyarit.finance.config;

import me.boonyarit.finance.resolver.CurrentUserArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("${app.cors.allowed-origins:http://localhost:3000}")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true);
    }
}
```

### 10. Update Application Configuration

#### Add Rate Limiting Configuration

Update `application.yaml`:

```yaml
# Add these configurations
app:
  rate-limit:
    requests-per-minute: 60
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

## Testing Enhancements

### 1. Test Refresh Token Flow

```bash
# 1. Login to get tokens
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password"
  }'

# 2. Use refresh token to get new access token
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

### 2. Test Current User Endpoint

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer your-jwt-token"
```

### 3. Test Rate Limiting

Send multiple requests quickly to auth endpoints to verify rate limiting is working.

## Implementation Checklist

### Final Checklist

- [ ] Create RefreshTokenEntity and repository
- [ ] Implement RefreshTokenService with CRUD operations
- [ ] Create @CurrentUser annotation and resolver
- [ ] Update AuthService with refresh token support
- [ ] Add refresh token endpoints to AuthController
- [ ] Implement rate limiting filter
- [ ] Configure CORS properly
- [ ] Add WebConfig for argument resolver
- [ ] Update application configuration
- [ ] Test all new functionality
- [ ] Update documentation

## Common Issues

### Refresh Token Storage

- Ensure refresh tokens are stored securely
- Consider encrypting refresh tokens in the database
- Implement proper cleanup for expired tokens

### Rate Limiting

- Consider using Redis for distributed rate limiting in production
- Implement different limits for different endpoints
- Add rate limit headers to responses

### CORS Configuration

- Be specific about allowed origins in production
- Consider environment-specific CORS settings

## Final Thoughts

After completing Phase 4, you will have a complete, production-ready authentication system with:

- JWT access tokens (short-lived)
- Refresh tokens (long-lived)
- Support for email/password and Google OAuth
- Rate limiting for security
- Proper CORS configuration
- Convenient access to authenticated users

## Time Estimate

Phase 4 should take approximately 5-6 hours to complete:

- 60 minutes: Refresh token entities and repository
- 90 minutes: Refresh token service implementation
- 45 minutes: @CurrentUser annotation and resolver
- 45 minutes: Update auth service and controller
- 60 minutes: Rate limiting implementation
- 45 minutes: CORS and web configuration
- 60 minutes: Testing and validation
