# Phase 1: Basic Security Configuration

## Overview

Phase 1 establishes the core security infrastructure for the authentication system. This includes Spring Security configuration, JWT token management, and the user repository setup.

## Tasks

### 1. Create User Repository

#### UserRepository Interface

**File**: `src/main/java/me/boonyarit/finance/repository/UserRepository.java`

```java
package me.boonyarit.finance.repository;

import me.boonyarit.finance.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### 2. Create JWT Service

#### JWT Utility Class

**File**: `src/main/java/me/boonyarit/finance/security/JwtService.java`

```java
package me.boonyarit.finance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import me.boonyarit.finance.enumeration.AuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails, AuthProvider provider) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("provider", provider.name());
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
```

### 3. Update UserEntity

#### Fix Authorities Implementation

**File**: `src/main/java/me/boonyarit/finance/entity/UserEntity.java`

Update the `getAuthorities()` method:

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
}
```

### 4. Create JWT Authentication Filter

#### JWT Filter Class

**File**: `src/main/java/me/boonyarit/finance/security/JwtAuthenticationFilter.java`

```java
package me.boonyarit.finance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        userEmail = jwtService.extractEmail(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### 5. Create UserDetailsService Implementation

#### UserDetailsService Class

**File**: `src/main/java/me/boonyarit/finance/service/UserService.java`

```java
package me.boonyarit.finance.service;

import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
```

### 6. Create Security Configuration

#### SecurityConfig Class

**File**: `src/main/java/me/boonyarit/finance/config/SecurityConfig.java`

```java
package me.boonyarit.finance.config;

import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.security.JwtAuthenticationFilter;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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

## Implementation Checklist

### Before Moving to Phase 2

- [ ] Create UserRepository interface
- [ ] Implement JwtService with token generation/validation
- [ ] Update UserEntity.getAuthorities() method
- [ ] Create JwtAuthenticationFilter
- [ ] Implement UserService as UserDetailsService
- [ ] Create SecurityConfig with proper configuration
- [ ] Test that application starts successfully
- [ ] Verify that endpoints are properly secured

## Testing Configuration

### Test Security Config

Create a test to verify the security configuration:

**File**: `src/test/java/me/boonyarit/finance/config/SecurityConfigTest.java`

```java
package me.boonyarit.finance.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.jwt.secret=test-secret-key-for-testing-only-123456789012345678901234567890",
    "app.jwt.expiration=900000"
})
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoderShouldEncodePassword() {
        String password = "testPassword";
        String encodedPassword = passwordEncoder.encode(password);

        assertNotEquals(password, encodedPassword);
        assertTrue(passwordEncoder.matches(password, encodedPassword));
    }
}
```

## Validation Steps

1. **Application Startup Check**:

   ```bash
   ./mvnw spring-boot:run
   ```

   Verify the application starts without errors.

2. **Security Test**:
   Try accessing a protected endpoint without authentication:

   ```bash
   curl -i http://localhost:8080/api/some-protected-endpoint
   ```

   Should return 401 Unauthorized.

3. **Public Endpoint Test**:

   ```bash
   curl -i http://localhost:8080/api/auth/login
   ```

   Should return 404 or 405 (not found or method not allowed) but not 401.

## Common Issues

### JWT Secret Length

JWT requires a secret key with at least 256 bits (32 bytes). Ensure your JWT_SECRET is long enough:

- Minimum: 32 characters
- Recommended: 64+ characters

### Bean Circular Dependencies

If you encounter circular dependency issues:

1. Use `@Lazy` annotation where appropriate
2. Review bean dependencies and restructure if needed
3. Consider using `@RequiredArgsConstructor` with Lombok

### Filter Order

Ensure JWT filter is registered before `UsernamePasswordAuthenticationFilter`:

```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

## Next Phase

After completing Phase 1, you'll have:

- Spring Security properly configured
- JWT token generation and validation
- User authentication infrastructure
- Proper password encoding

Phase 2 will build on this foundation to implement email/password authentication endpoints.

## Time Estimate

Phase 1 should take approximately 2-3 hours to complete:

- 30 minutes: UserRepository and UserDetailsService
- 45 minutes: JwtService implementation
- 30 minutes: JwtAuthenticationFilter
- 45 minutes: SecurityConfig setup
- 30 minutes: Testing and validation
