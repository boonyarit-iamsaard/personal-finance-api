# Authentication API Endpoints Reference

## Overview

This document provides a comprehensive reference for all authentication-related API endpoints in the Personal Finance API.

## Base URL

```text
Production: https://api.yourfinance.com/api/auth
Development: http://localhost:8080/api/auth
```

## Authentication

All endpoints except authentication endpoints require a valid JWT token in the Authorization header:

```text
Authorization: Bearer <JWT_TOKEN>
```

## Rate Limiting

Authentication endpoints are rate-limited to 60 requests per minute per IP address.

## Endpoints

### 1. Register New User

Register a new user with email and password.

**Endpoint**: `POST /api/auth/register`

**Request Body**:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123"
}
```

**Response**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "provider": "LOCAL"
}
```

**Status Codes**:

- `201 Created` - User registered successfully
- `400 Bad Request` - Validation errors
- `409 Conflict` - Email already exists

**Validation Rules**:

- `firstName`: Required, non-empty string
- `lastName`: Required, non-empty string
- `email`: Required, valid email format, must be unique
- `password`: Required, minimum 8 characters

---

### 2. Login (Email/Password)

Authenticate with email and password.

**Endpoint**: `POST /api/auth/login`

**Request Body**:

```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123"
}
```

**Response**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "provider": "LOCAL"
}
```

**Status Codes**:

- `200 OK` - Authentication successful
- `400 Bad Request` - Validation errors
- `401 Unauthorized` - Invalid credentials

---

### 3. Refresh Access Token

Use a refresh token to obtain a new access token.

**Endpoint**: `POST /api/auth/refresh-token`

**Request Body**:

```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "660e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "provider": "LOCAL"
}
```

**Status Codes**:

- `200 OK` - Token refreshed successfully
- `400 Bad Request` - Invalid refresh token
- `401 Unauthorized` - Refresh token expired or revoked

---

### 4. Logout

Invalidate the current session by revoking the refresh token.

**Endpoint**: `POST /api/auth/logout`

**Request Body**:

```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response**:

```json
No content (204 No Content)
```

**Status Codes**:

- `204 No Content` - Logout successful
- `400 Bad Request` - Invalid refresh token

---

### 5. Get Current User

Retrieve information about the currently authenticated user.

**Endpoint**: `GET /api/auth/me`

**Headers**:

```text
Authorization: Bearer <JWT_TOKEN>
```

**Response**:

```json
{
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "provider": "LOCAL",
  "role": "USER"
}
```

**Status Codes**:

- `200 OK` - User information retrieved
- `401 Unauthorized` - Invalid or missing token

---

### 6. Get Google OAuth URL

Get the authorization URL for Google OAuth2 login.

**Endpoint**: `GET /api/auth/oauth/google/url`

**Response**:

```json
{
  "url": "https://accounts.google.com/o/oauth2/auth?client_id=...&redirect_uri=...&response_type=code&scope=email profile",
  "redirectUri": "http://localhost:8080/api/auth/oauth/callback/google"
}
```

**Status Codes**:

- `200 OK` - OAuth URL generated successfully

---

### 7. Google OAuth2 Authorization

Initiate Google OAuth2 authorization flow.

**Endpoint**: `GET /oauth2/authorization/google`

**Query Parameters** (optional):

- `redirect_uri` - Custom redirect URI after successful authentication
- `state` - CSRF protection state parameter

**Flow**:

1. Redirect to Google's authorization page
2. User authenticates with Google
3. Redirect back to callback URL with JWT token

**Callback Response Options**:

For API requests (Accept: application/json):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@gmail.com",
  "firstName": "John",
  "lastName": "Doe",
  "provider": "GOOGLE"
}
```

For web requests:
Redirect to configured URL with token as query parameter:

```text
http://localhost:3000/auth/callback?token=eyJhbGciOiJIUzI1NiJ9...
```

---

### 8. Google OAuth2 Callback

OAuth2 callback endpoint for handling Google's authorization response.

**Endpoint**: `GET /api/auth/oauth/callback/google`

**Query Parameters**:

- `code` - Authorization code from Google
- `state` - State parameter for CSRF protection
- `error` - Error code if authorization failed

**Response**:
Same as Google OAuth2 Authorization response above.

**Status Codes**:

- `200 OK` - Authentication successful
- `401 Unauthorized` - Authentication failed
- `302 Found` - Redirect with error (for web flows)

## Error Responses

### Standard Error Format

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "timestamp": "2024-01-01T12:00:00Z"
  }
}
```

### Validation Error Format

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "timestamp": "2024-01-01T12:00:00Z",
    "fieldErrors": {
      "email": "Invalid email format",
      "password": "Password must be at least 8 characters long"
    }
  }
}
```

### Common Error Codes

| Code                    | HTTP Status | Description                    |
| ----------------------- | ----------- | ------------------------------ |
| `INVALID_CREDENTIALS`   | 401         | Invalid email or password      |
| `TOKEN_EXPIRED`         | 401         | JWT token has expired          |
| `TOKEN_INVALID`         | 401         | JWT token is invalid           |
| `REFRESH_TOKEN_EXPIRED` | 401         | Refresh token has expired      |
| `REFRESH_TOKEN_REVOKED` | 401         | Refresh token has been revoked |
| `USER_NOT_FOUND`        | 404         | User not found                 |
| `EMAIL_ALREADY_EXISTS`  | 409         | Email is already registered    |
| `VALIDATION_ERROR`      | 400         | Request validation failed      |
| `OAUTH2_ERROR`          | 401         | OAuth2 authentication failed   |
| `RATE_LIMIT_EXCEEDED`   | 429         | Too many requests              |

## JWT Token Format

### Header

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### Payload

```json
{
  "sub": "john.doe@example.com",
  "provider": "LOCAL",
  "iat": 1640995200,
  "exp": 1640996100
}
```

## SDK Examples

### JavaScript/TypeScript

```typescript
class AuthClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  async register(userData: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }) {
    const response = await fetch(`${this.baseUrl}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(userData),
    });
    return response.json();
  }

  async login(email: string, password: string) {
    const response = await fetch(`${this.baseUrl}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    return response.json();
  }

  async refreshToken(refreshToken: string) {
    const response = await fetch(`${this.baseUrl}/refresh-token`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    return response.json();
  }

  async getGoogleOAuthUrl() {
    const response = await fetch(`${this.baseUrl}/oauth/google/url`);
    return response.json();
  }
}

// Usage
const auth = new AuthClient("http://localhost:8080/api/auth");

// Register
const user = await auth.register({
  firstName: "John",
  lastName: "Doe",
  email: "john@example.com",
  password: "SecurePass123",
});

// Store tokens
localStorage.setItem("accessToken", user.token);
localStorage.setItem("refreshToken", user.refreshToken);
```

### Python

```python
import requests

class AuthClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.session = requests.Session()

    def register(self, first_name, last_name, email, password):
        response = self.session.post(f"{self.base_url}/register", json={
            "firstName": first_name,
            "lastName": last_name,
            "email": email,
            "password": password
        })
        response.raise_for_status()
        return response.json()

    def login(self, email, password):
        response = self.session.post(f"{self.base_url}/login", json={
            "email": email,
            "password": password
        })
        response.raise_for_status()
        return response.json()

    def get_current_user(self, token):
        headers = {"Authorization": f"Bearer {token}"}
        response = self.session.get(f"{self.base_url}/me", headers=headers)
        response.raise_for_status()
        return response.json()

# Usage
auth = AuthClient("http://localhost:8080/api/auth")

# Login
result = auth.login("john@example.com", "SecurePass123")
token = result["token"]

# Get current user
user = auth.get_current_user(token)
print(f"Welcome, {user['firstName']}!")
```

## Testing

### Using curl

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "password": "SecurePass123"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123"
  }'

# Get current user (replace TOKEN with actual JWT)
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer TOKEN"
```

## Environment Variables

| Variable               | Description                                             | Required  |
| ---------------------- | ------------------------------------------------------- | --------- |
| `JWT_SECRET`           | Secret key for JWT signing                              | Yes       |
| `JWT_EXPIRATION`       | JWT token expiration in ms (default: 900000)            | No        |
| `GOOGLE_CLIENT_ID`     | Google OAuth2 client ID                                 | For OAuth |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret                             | For OAuth |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins (default: <http://localhost:3000>) | No        |
