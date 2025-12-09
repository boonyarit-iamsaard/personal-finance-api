# Google OAuth2 Setup Guide

## Overview

This guide walks you through setting up Google OAuth2 authentication for the Personal Finance API. You'll need to create a project in Google Cloud Console and configure OAuth2 credentials.

## Prerequisites

- Google account
- Access to Google Cloud Console (<https://console.cloud.google.com>)

## Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Sign in with your Google account
3. Click on the project dropdown at the top
4. Click **"NEW PROJECT"**
5. Enter a project name (e.g., "Personal Finance API")
6. Click **"CREATE"**

## Step 2: Enable Google+ API

1. In your new project, go to the navigation menu (☰)
2. Select **"APIs & Services"** > **"Library"**
3. Search for "Google+ API"
4. Click on **"Google+ API"**
5. Click **"ENABLE"**
6. Also enable **"People API"** for additional user information

## Step 3: Create OAuth2 Consent Screen

1. Go to **"APIs & Services"** > **"OAuth consent screen"**
2. Choose **"External"** (unless you have Google Workspace)
3. Click **"CREATE"**

### Fill in the consent screen

**App Information**:

- **App name**: Personal Finance API
- **User support email**: <your-email@example.com>
- **App logo** (optional): Upload your app logo
- **App homepage**: <https://your-domain.com>
- **App privacy policy link**: <https://your-domain.com/privacy>
- **App terms of service link** (optional): <https://your-domain.com/terms>
- **Authorized domains**:
  - localhost (for development)
  - your-domain.com (for production)

**Developer Contact Information**:

- **Email addresses**: <your-email@example.com>

1. Click **"SAVE AND CONTINUE"**

### Scopes (Step 2)

1. Click **"+ ADD OR REMOVE SCOPES"**
2. Add these scopes:
   - `.../auth/userinfo.email`
   - `.../auth/userinfo.profile`
   - `openid`
3. Click **"UPDATE"**
4. Click **"SAVE AND CONTINUE"**

### Test Users (Step 3)

1. Add test users (your email address)
2. Click **"SAVE AND CONTINUE"**

## Step 4: Create OAuth2 Credentials

1. Go to **"APIs & Services"** > **"Credentials"**
2. Click **"+ CREATE CREDENTIALS"**
3. Select **"OAuth client ID"**

### Configure OAuth Client

**Application type**: Select **"Web application"**

**Name**: Personal Finance API Auth

**Authorized JavaScript origins**:

- `http://localhost:3000` (for development)
- `https://your-frontend-domain.com` (for production)

**Authorized redirect URIs**:

- `http://localhost:8080/api/auth/oauth/callback/google`
- `https://your-api-domain.com/api/auth/oauth/callback/google`

1. Click **"CREATE"**

### Important: Save Your Credentials

You will receive:

- **Client ID**: (starts with `.googleusercontent.com`)
- **Client Secret**: (random string)

**⚠️ Important**: Copy these values immediately. You won't be able to see the Client Secret again after closing this dialog.

## Step 5: Configure Your Application

### Environment Variables

Set these environment variables in your application:

```bash
# For development
export GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your-client-secret"

# For production, use your secure method of storing secrets
```

### application.yaml Configuration

Update your `application.yaml`:

```yaml
spring:
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
```

## Step 6: Test the Integration

### 1. Start Your Application

```bash
./mvnw spring-boot:run
```

### 2. Get Google OAuth URL

```bash
curl -X GET http://localhost:8080/api/auth/oauth/google/url
```

### 3. Test OAuth Flow

1. Copy the URL from the response
2. Paste it in your browser
3. Sign in with your Google account
4. You should be redirected back with a JWT token

### 4. Verify Token

```bash
# Use the returned token to access protected endpoints
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Troubleshooting

### Common Issues

#### 1. Redirect URI Mismatch

**Error**: `redirect_uri_mismatch`

**Solution**:

- Ensure the exact redirect URI is configured in Google Cloud Console
- Check for trailing slashes
- Verify the protocol (http vs https)
- Make sure port numbers match

#### 2. Invalid Client

**Error**: `invalid_client`

**Solution**:

- Verify Client ID and Client Secret are correct
- Check for extra spaces or special characters
- Ensure environment variables are properly set

#### 3. Access Denied

**Error**: `access_denied`

**Solution**:

- Check if your email is added as a test user
- Verify the OAuth consent screen is properly configured
- Ensure required scopes are added

#### 4. CORS Issues

**Error**: CORS errors in browser

**Solution**:

- Update CORS configuration in your application
- Add your frontend domain to authorized JavaScript origins
- Check preflight request handling

### Debug Mode

For debugging, you can enable OAuth2 logging:

Add to `application.yaml`:

```yaml
logging:
  level:
    org.springframework.security.oauth2: DEBUG
    org.springframework.security.web: DEBUG
```

## Production Considerations

### 1. HTTPS Required

For production:

- All URLs must use HTTPS
- Update authorized origins and URIs to use HTTPS
- Obtain SSL certificates for your domains

### 2. Security Best Practices

- **Store secrets securely**: Use a secure secrets management system
- **Domain verification**: Verify your domain in Google Search Console
- **App review**: Submit your app for Google OAuth verification
- **Rate limiting**: Implement proper rate limiting
- **Monitoring**: Monitor OAuth2 usage and errors

### 3. Google OAuth Verification

To remove the "unverified app" screen:

1. Go to **"OAuth consent screen"**
2. Click **"PUBLISH APP"**
3. Complete the verification process
4. Submit for review by Google

## Multiple Environments

### Development Environment

```yaml
# application-dev.yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID_DEV}
            client-secret: ${GOOGLE_CLIENT_SECRET_DEV}
```

### Production Environment

```yaml
# application-prod.yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID_PROD}
            client-secret: ${GOOGLE_CLIENT_SECRET_PROD}
```

## Additional Configuration Options

### Custom Scopes

You can request additional scopes:

```yaml
scope: email, profile, https://www.googleapis.com/auth/calendar.readonly
```

### Custom Provider Configuration

```yaml
provider:
  google:
    authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
    token-uri: https://oauth2.googleapis.com/token
    user-info-uri: https://www.googleapis.com/oauth2/v2/userinfo
    user-name-attribute: email
```

## Next Steps

After setting up Google OAuth2:

1. **Implement account linking**: Allow users to link Google accounts to existing email accounts
2. **Add other OAuth providers**: Consider adding GitHub, Facebook, etc.
3. **Implement role-based access**: Use OAuth2 attributes to assign roles
4. **Add audit logging**: Track OAuth2 login events
5. **Implement 2FA**: Add two-factor authentication for security

## References

- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Spring Security OAuth2 Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/)
- [Google Cloud Console](https://console.cloud.google.com)

## Support

If you encounter issues:

1. Check the application logs for detailed error messages
2. Verify all configurations match exactly
3. Ensure your Google Cloud project has the correct APIs enabled
4. Contact Google Cloud support for platform issues
