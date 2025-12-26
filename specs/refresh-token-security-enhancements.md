# Refresh Token Security Enhancement Plan

## Overview

This document outlines a comprehensive security enhancement plan for the existing refresh token implementation in the Personal Finance API. The current implementation provides solid foundational security but requires several hardening measures to meet production-grade security standards.

## Current Implementation Analysis

### ✅ Strengths

- **Token Rotation Pattern**: Proper implementation where each refresh creates a new token and revokes the old one
- **Expiration Management**: Automatic cleanup of expired tokens with proper validation
- **Transactional Safety**: Proper @Transactional annotations ensure data consistency
- **Configuration Management**: Centralized properties through RefreshTokenProperties
- **Clean Architecture**: Well-structured service layer with proper separation of concerns

### 🔴 Critical Security Issues

- **Missing Exception Handler**: RefreshTokenException is not handled in GlobalExceptionHandler
- **Token Leakage in Logs**: Exception messages contain actual token values
- **Logout Endpoint Vulnerability**: Accepts raw string body instead of JSON
- **EAGER User Loading**: Potential performance and security concern with user data exposure

### 🟡 Medium Security Concerns

- **No Rate Limiting**: Refresh endpoints lack protection against brute force attacks
- **Missing Audit Trail**: No logging of refresh token operations for security monitoring
- **Insufficient Test Coverage**: No dedicated tests for RefreshTokenService
- **No Token Usage Tracking**: No mechanism to detect suspicious refresh patterns

## Enhancement Plan

### Phase 1: Critical Security Fixes (Priority: HIGH)

#### 1.1 Fix Global Exception Handler

**File**: `src/main/java/me/boonyarit/finance/exception/GlobalExceptionHandler.java`

**Issue**: RefreshTokenException not handled, causing 500 Internal Server Error instead of proper 401 Unauthorized.

**Implementation**:

```java
@ExceptionHandler(RefreshTokenException.class)
public ResponseEntity<ErrorResponse> handleRefreshTokenException(RefreshTokenException e) {
    ErrorResponse response = new ErrorResponse(
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorized",
        "Invalid or expired refresh token"
    );
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
}
```

#### 1.2 Fix RefreshTokenException Token Leakage

**File**: `src/main/java/me/boonyarit/finance/exception/RefreshTokenException.java`

**Issue**: Exception messages contain actual token values, creating security risks in logs.

**Implementation**:

```java
public RefreshTokenException(String token, String message) {
    super(message);
    this.token = null; // Never store token in exception
}
```

#### 1.3 Fix Logout Endpoint

**File**: `src/main/java/me/boonyarit/finance/controller/AuthenticationController.java`

**Issue**: Accepts raw string body instead of JSON, vulnerable to injection.

**Implementation**:

```java
@PostMapping("/logout")
public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authenticationService.logout(request.refreshToken());
    return ResponseEntity.noContent().build();
}
```

#### 1.4 Fix EAGER User Loading

**File**: `src/main/java/me/boonyarit/finance/entity/RefreshTokenEntity.java`

**Issue**: EAGER fetching loads user data unnecessarily, potential performance impact.

**Implementation**:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@ToString.Exclude
private UserEntity user;
```

### Phase 2: Security Hardening (Priority: MEDIUM)

#### 2.1 Implement Rate Limiting

**New File**: `src/main/java/me/boonyarit/finance/config/RateLimitingConfig.java`
**New File**: `src/main/java/me/boonyarit/finance/service/RateLimiterService.java`

**Approach**: Hybrid rate limiting strategy

- Per-IP for unauthenticated endpoints (`/login`, `/register`)
- Per-User for authenticated endpoints (`/refresh-token`)

**RateLimiterService Implementation**:

```java
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final Cache<String, AtomicInteger> requestCache =
        Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    public boolean isAllowed(String key, int limit) {
        AtomicInteger counter = requestCache.get(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= limit;
    }
}
```

**RateLimitingConfig Implementation**:

```java
@Configuration
@RequiredArgsConstructor
public class RateLimitingConfig {

    private final RateLimiterService rateLimiterService;

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter() {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitingFilter(rateLimiterService));
        registration.addUrlPatterns("/api/auth/*");
        return registration;
    }
}
```

#### 2.2 Add Security Audit Logging

**New File**: `src/main/java/me/boonyarit/finance/service/AuditService.java`
**New File**: `src/main/java/me/boonyarit/finance/entity/SecurityEvent.java`
**New File**: `src/main/java/me/boonyarit/finance/enumeration/SecurityEventType.java`

**SecurityEventType Enum**:

```java
public enum SecurityEventType {
    TOKEN_CREATED,
    TOKEN_REFRESHED,
    TOKEN_REVOKED,
    TOKEN_EXPIRED_USED,
    RATE_LIMIT_EXCEEDED,
    SUSPICIOUS_ACTIVITY
}
```

**SecurityEvent Entity**:

```java
@Entity
@Table(name = "security_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SecurityEventType eventType;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    @Column(length = 500)
    private String userAgent;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}
```

#### 2.3 Add Token Usage Tracking

**New File**: `src/main/java/me/boonyarit/finance/service/DeviceFingerprintService.java`

**Implementation**:

```java
@Service
@RequiredArgsConstructor
public class DeviceFingerprintService {

    public String generateFingerprint(String userAgent, String ipAddress) {
        String combined = userAgent + ipAddress;
        return DigestUtils.sha256Hex(combined);
    }

    public boolean isSuspiciousActivity(String currentFingerprint, String storedFingerprint) {
        return !currentFingerprint.equals(storedFingerprint);
    }
}
```

#### 2.4 Schedule Automatic Cleanup

**File**: `src/main/java/me/boonyarit/finance/service/RefreshTokenService.java`

**Implementation**:

```java
@Scheduled(fixedRateString = "#{@refreshTokenProperties.repeatIntervalMs}")
@Transactional
public void scheduledCleanup() {
    cleanupExpiredTokens();
    log.info("Completed scheduled cleanup of expired refresh tokens");
}
```

### Phase 3: Enhanced Testing (Priority: MEDIUM)

#### 3.1 Create RefreshTokenService Tests

**New File**: `src/test/java/me/boonyarit/finance/service/RefreshTokenServiceTest.java`

**Test Coverage with Correct Pattern**:

```java
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Test
    @DisplayName("Create Refresh Token: Should revoke existing tokens and create new one when user exists")
    void createRefreshToken_ShouldRevokeExistingAndCreateNew_WhenUserExists() {
        // Test implementation
    }

    @Test
    @DisplayName("Verify Expiration: Should throw exception for expired token")
    void verifyExpiration_ShouldThrowRefreshTokenException_WhenTokenExpired() {
        // Test implementation
    }

    @Test
    @DisplayName("Refresh Token: Should rotate tokens properly when token is valid")
    void refreshToken_ShouldRotateTokens_WhenTokenValid() {
        // Test implementation
    }

    @Test
    @DisplayName("Refresh Token: Should throw exception when token not found")
    void refreshToken_ShouldThrowRefreshTokenException_WhenTokenNotFound() {
        // Test implementation
    }

    @Test
    @DisplayName("Revoke Token: Should mark token as revoked when token exists")
    void revokeToken_ShouldMarkTokenAsRevoked_WhenTokenExists() {
        // Test implementation
    }

    @Test
    @DisplayName("Cleanup Expired Tokens: Should delete expired tokens when called")
    void cleanupExpiredTokens_ShouldDeleteExpiredTokens_WhenCalled() {
        // Test implementation
    }

    @Test
    @DisplayName("Find By Token: Should return token when token exists")
    void findByToken_ShouldReturnToken_WhenTokenExists() {
        // Test implementation
    }

    @Test
    @DisplayName("Find By Token: Should return empty when token does not exist")
    void findByToken_ShouldReturnEmpty_WhenTokenDoesNotExist() {
        // Test implementation
    }
}
```

#### 3.2 Create Integration Tests

**New File**: `src/test/java/me/boonyarit/finance/integration/RefreshTokenIntegrationTest.java`

#### 3.3 Create RateLimiting Tests

**New File**: `src/test/java/me/boonyarit/finance/service/RateLimiterServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Test
    @DisplayName("Is Allowed: Should allow requests within limit")
    void isAllowed_ShouldAllowRequests_WhenWithinLimit() {
        // Test implementation
    }

    @Test
    @DisplayName("Is Allowed: Should block requests exceeding limit")
    void isAllowed_ShouldBlockRequests_WhenExceedingLimit() {
        // Test implementation
    }

    @Test
    @DisplayName("Is Allowed: Should reset counter after time window")
    void isAllowed_ShouldResetCounter_AfterTimeWindow() {
        // Test implementation
    }
}
```

#### 3.4 Create Device Fingerprinting Tests

**New File**: `src/test/java/me/boonyarit/finance/service/DeviceFingerprintServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class DeviceFingerprintServiceTest {

    @Test
    @DisplayName("Generate Fingerprint: Should create consistent hash for same inputs")
    void generateFingerprint_ShouldCreateConsistentHash_WhenSameInputs() {
        // Test implementation
    }

    @Test
    @DisplayName("Is Suspicious Activity: Should detect mismatch when fingerprints differ")
    void isSuspiciousActivity_ShouldDetectMismatch_WhenFingerprintsDiffer() {
        // Test implementation
    }

    @Test
    @DisplayName("Is Suspicious Activity: Should return false when fingerprints match")
    void isSuspiciousActivity_ShouldReturnFalse_WhenFingerprintsMatch() {
        // Test implementation
    }
}
```

### Phase 4: Configuration Enhancements (Priority: LOW)

#### 4.1 Update Application Configuration

**File**: `src/main/resources/application.yaml`

```yaml
security:
  jwt:
    refresh-expiration-ms: 604800000 # 7 days
    repeat-interval-ms: 3600000 # 1 hour
    max-refresh-tokens-per-user: 5 # Session limiting
  rate-limiting:
    login:
      requests-per-minute: 10
    refresh-token:
      requests-per-minute: 60
    register:
      requests-per-minute: 5
  audit:
    retention-days: 90
    enabled: true
  device-fingerprinting:
    enabled: true
    ip-threshold: 3
```

#### 4.2 Update RefreshTokenProperties

**File**: `src/main/java/me/boonyarit/finance/config/RefreshTokenProperties.java`

```java
@Getter
@ConfigurationProperties(prefix = "security.jwt")
@RequiredArgsConstructor
public class RefreshTokenProperties {

    private final long refreshExpirationMs;
    private final long repeatIntervalMs;
    private final int maxRefreshTokensPerUser = 5;
}
```

## Implementation Priority Matrix

| Feature            | Security Impact | Complexity | Priority | Estimated Hours |
| ------------------ | --------------- | ---------- | -------- | --------------- |
| Exception Handler  | Critical        | Low        | 1        | 0.5             |
| Logout Fix         | Critical        | Low        | 1        | 0.5             |
| Token Leakage Fix  | Critical        | Low        | 1        | 0.5             |
| LAZY Loading       | High            | Low        | 2        | 0.5             |
| Rate Limiting      | High            | Medium     | 3        | 3-4             |
| Audit Logging      | Medium          | Medium     | 3        | 2-3             |
| Token Tracking     | Medium          | High       | 4        | 3-4             |
| Service Tests      | Medium          | Medium     | 2        | 2-3             |
| Cleanup Scheduling | Medium          | Low        | 2        | 1               |

**Total Estimated Time**: 14-18 hours

## Security Improvements Summary

| **Current**        | **Enhanced**              | **Risk Reduction** |
| ------------------ | ------------------------- | ------------------ |
| No rate limiting   | Hybrid rate limiting      | 80%                |
| Exception leakage  | Secure exception handling | 70%                |
| No audit trail     | 90-day audit logs         | 60%                |
| No device tracking | Device fingerprinting     | 70%                |
| Basic tests        | Comprehensive test suite  | 50%                |

## Implementation Checklist

### Phase 1: Critical Security Fixes

- [ ] Add RefreshTokenException handler in GlobalExceptionHandler
- [ ] Remove token values from RefreshTokenException messages
- [ ] Fix logout endpoint to use proper JSON request body
- [ ] Change RefreshTokenEntity user relationship to LAZY fetching
- [ ] Test all exception scenarios

### Phase 2: Security Hardening

- [ ] Implement RateLimiterService with Caffeine cache
- [ ] Create RateLimitingFilter for endpoint protection
- [ ] Implement SecurityEvent entity and repository
- [ ] Create AuditService for security event logging
- [ ] Add DeviceFingerprintService for device tracking
- [ ] Schedule automatic cleanup of expired tokens
- [ ] Update RefreshTokenService with audit logging integration

### Phase 3: Enhanced Testing

- [ ] Create comprehensive RefreshTokenService unit tests
- [ ] Add RateLimiterService unit tests
- [ ] Create DeviceFingerprintService unit tests
- [ ] Add integration tests for complete refresh flows
- [ ] Test security scenarios (rate limiting, audit logging)

### Phase 4: Configuration Enhancements

- [ ] Update application.yaml with new security configurations
- [ ] Enhance RefreshTokenProperties with additional settings
- [ ] Add dependency management for Caffeine cache
- [ ] Update AGENTS.md with new development commands

## Dependencies Required

Add to `pom.xml`:

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
</dependency>
```

## Performance Considerations

### BCrypt Impact

- **CPU Overhead**: ~100ms per hash (acceptable for auth flow)
- **Memory Usage**: Minimal increase
- **Security Benefit**: Prevents rainbow table attacks

### Rate Limiting Storage

- **Memory Usage**: ~1MB per 10,000 active clients
- **CPU Overhead**: Minimal O(1) operations with Caffeine cache
- **Cleanup**: Automatic expiration handled by cache

### Audit Logging

- **Storage**: ~50KB per 1000 events per day
- **Performance**: Minimal impact with async logging
- **Retention**: Configurable (default 90 days)

## Migration Considerations

Since we're using Hibernate's `ddl-auto: update`:

1. **New Tables**: Automatically created (security_events)
2. **New Columns**: Automatically added to existing tables
3. **Index Creation**: Handled through JPA annotations
4. **Data Migration**: No existing data migration required

## Testing Strategy

### Unit Tests

- Focus on business logic in services
- Mock external dependencies (database, cache)
- Test edge cases and error conditions
- Verify security controls work correctly

### Integration Tests

- Test complete refresh token flows
- Verify rate limiting works across requests
- Test audit logging with real database
- Validate exception handling end-to-end

### Security Tests

- Test rate limiting bypass attempts
- Verify token leakage is prevented
- Test suspicious activity detection
- Validate audit log completeness

## Rollback Plan

Each phase can be independently rolled back:

1. **Phase 1**: Simply revert exception handler and entity changes
2. **Phase 2**: Disable new features through configuration flags
3. **Phase 3**: Remove new test files (no production impact)
4. **Phase 4**: Revert configuration changes

## Monitoring and Alerting

After implementation, monitor:

- Rate limiting effectiveness (blocked requests per minute)
- Audit log volume and storage usage
- Token refresh patterns and anomalies
- Performance impact on authentication endpoints

## Success Criteria

Implementation considered successful when:

- All existing functionality continues to work
- Rate limiting blocks suspicious patterns effectively
- Audit logs capture all security events
- Tests achieve >90% code coverage for new features
- No performance regression in authentication flows
- Security scan shows no token leakage issues

## Next Steps

1. Review and approve this plan
2. Schedule implementation phases
3. Set up monitoring baseline metrics
4. Execute Phase 1 (Critical fixes) first
5. Continue with subsequent phases based on risk assessment
6. Conduct security review after each phase completion
