# Authentication System Overview

## Introduction

This document outlines the authentication system for the Personal Finance API. The system supports both traditional email/password authentication and Google OAuth2 authentication, using JWT tokens for stateless session management.

## Architecture

### Authentication Flow

```text
┌─────────────┐     ┌───────────────┐     ┌────────────┐     ┌──────────────┐
│   Client    │────▶│ AuthController│────▶│AuthService │────▶│UserRepository│
└─────────────┘     └───────────────┘     └────────────┘     └──────────────┘
                           │                    │
                           ▼                    ▼
                    ┌─────────────┐     ┌─────────────┐
                    │  JwtService │     │ UserEntity  │
                    └─────────────┘     └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ JWT Token   │
                    └─────────────┘
```

### Components

1. **AuthController**: REST endpoints for authentication
2. **AuthenticationService**: Business logic for authentication
3. **JwtService**: JWT token generation and validation
4. **SecurityConfig**: Spring Security configuration
5. **JwtAuthenticationFilter**: JWT token validation filter

### Authentication Methods

1. **Email/Password**: Traditional authentication with BCrypt password encoding
2. **Google OAuth2**: OAuth2 flow with Google as identity provider

## JWT Token Structure

### Access Token Payload

```json
{
  "sub": "user@example.com",
  "email": "user@example.com",
  "provider": "LOCAL",
  "iat": 1640995200,
  "exp": 1640996100
}
```

### Token Configuration

- **Algorithm**: HS256
- **Expiration**: 15 minutes
- **Secret**: Configurable in application.yaml
- **Claims**: Email, provider, issued at, expiration

## Security Considerations

### Password Security

- BCrypt encoding with default strength (10)
- Minimum password length: 8 characters
- Password validation on registration

### Token Security

- Stateless JWT tokens
- Short expiration time
- HTTPS required in production
- Optional refresh tokens (Phase 4)

### OAuth2 Security

- State parameter to prevent CSRF
- PKCE (Proof Key for Code Exchange)
- Secure redirection URI validation

## User Model

### UserEntity Fields

- `id`: Primary key (inherited from BaseEntity)
- `email`: Unique email address (username)
- `password`: Encrypted password (nullable for OAuth users)
- `firstName`: User's first name
- `lastName`: User's last name
- `role`: User role (ADMIN/USER)
- `provider`: Authentication provider (LOCAL/GOOGLE)
- `createdAt`: Account creation timestamp
- `updatedAt`: Last update timestamp

### Authentication Providers

- **LOCAL**: Email/password authentication
- **GOOGLE**: Google OAuth2 authentication

## Implementation Phases

1. **Phase 0**: Foundation setup and dependency configuration
2. **Phase 1**: Basic security configuration with JWT
3. **Phase 2**: Email/password authentication implementation
4. **Phase 3**: Google OAuth2 integration
5. **Phase 4**: Advanced features and enhancements

## API Endpoints

### Authentication Endpoints

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login with email/password
- `POST /api/auth/oauth/google` - Google OAuth2 callback
- `GET /api/auth/me` - Get current user info

### Protected Endpoints

All endpoints under `/api/**` require valid JWT token, except for authentication endpoints.

## Error Handling

### Authentication Errors

- **401 Unauthorized**: Invalid or expired token
- **403 Forbidden**: Insufficient permissions
- **409 Conflict**: Email already exists
- **400 Bad Request**: Invalid input data

### Error Response Format

```json
{
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password",
    "timestamp": "2024-01-01T12:00:00Z"
  }
}
```

## Configuration

### Application Properties

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:your-secret-key}
    expiration: 900000 # 15 minutes

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
```

## Testing Strategy

### Unit Tests

- JWT service token generation and validation
- Authentication service business logic
- Password encoding and verification

### Integration Tests

- AuthController endpoints
- OAuth2 flow simulation
- Security configuration validation

## Future Enhancements

1. **Refresh Tokens**: Long-lived refresh tokens for better UX
2. **Multi-Factor Authentication**: TOTP or SMS-based 2FA
3. **Social Logins**: Additional OAuth2 providers (Facebook, GitHub)
4. **Account Management**: Email verification, password reset
5. **Rate Limiting**: Prevent brute force attacks
6. **Audit Logging**: Track authentication events
