# Phase 0: Foundation Setup

## Overview

Phase 0 focuses on preparing the project foundation for implementing the authentication system. This includes fixing existing issues, adding necessary dependencies, and setting up the project structure.

## Tasks

### 1. ✅ Fix Enum Typos

#### Role Enum

**File**: `src/main/java/me/boonyarit/finance/enumeration/Role.java`

**Current Issue**: Contains typo "ADIMIN" instead of "ADMIN"

```java
// BEFORE (with typo)
public enum Role {
    ADIMIN,  // ← This should be ADMIN
    USER
}

// AFTER (fixed)
public enum Role {
    ADMIN,
    USER
}
```

#### AuthProvider Enum

**File**: `src/main/java/me/boonyarit/finance/enumeration/AuthProvider.java`

**Current Issue**: Contains typo "GOOSE" instead of "GOOGLE"

```java
// BEFORE (with typo)
public enum AuthProvider {
    LOCAL,
    GOOSE    // ← This should be GOOGLE
}

// AFTER (fixed)
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
```

### 2. ✅ Add Dependencies

#### Update pom.xml

Add the following dependencies to support JWT, OAuth2, OpenAPI, Flyway, and MapStruct:

```xml
<!-- JWT Support -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- OAuth2 Client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- OpenAPI / Swagger -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- Database Migration -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

### 3. ✅ Update Application Configuration

#### application.yaml

Add JWT configuration properties and enable Flyway:

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
      ddl-auto: validate # Changed from update to validate for Flyway
    open-in-view: false
    show-sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true

# JWT Configuration
app:
  jwt:
    secret: ${JWT_SECRET:mySecretKey123456789012345678901234567890}
    expiration: 900000 # 15 minutes in milliseconds

# OAuth2 Configuration (will be configured in Phase 3)
# spring:
#   security:
#     oauth2:
#       client:
#         registration:
#           google:
#             client-id: ${GOOGLE_CLIENT_ID}
#             client-secret: ${GOOGLE_CLIENT_SECRET}
```

### 4. Create Package Structure

Create the following package structure under `me.boonyarit.finance`:

```text
src/main/java/me/boonyarit/finance/
├── config/         # Security configuration
├── controller/     # REST controllers
├── dto/            # Data Transfer Objects
│   ├── request/    # Request DTOs
│   └── response/   # Response DTOs
├── exception/      # Custom exceptions
├── mapper/         # MapStruct mappers
├── repository/     # JPA repositories
├── security/       # Security-related utilities
└── service/        # Business logic
```

### 5. Create Initial Migration

Create the initial Flyway migration script:

**File**: `src/main/resources/db/migration/V1__init_schema.sql`

```sql
CREATE SEQUENCE primary_key_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE users (
    id BIGINT NOT NULL DEFAULT nextval('primary_key_seq'),
    created_date TIMESTAMP(6),
    last_modified_date TIMESTAMP(6),
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255),
    PRIMARY KEY (id)
);

ALTER TABLE users ADD CONSTRAINT uc_users_email UNIQUE (email);
```

### 6. Create README for Authentication

Create a documentation file that will be updated throughout the phases:

**File**: `docs/authentication.md`

````markdown
# Authentication Implementation

This document tracks the implementation of the authentication system.

## Phase Status

- [x] Phase 0: Foundation Setup
- [ ] Phase 1: Basic Security Configuration
- [ ] Phase 2: Email/Password Authentication
- [ ] Phase 3: Google OAuth Integration
- [ ] Phase 4: Authentication Enhancements

## Configuration Required

### Environment Variables

- `JWT_SECRET`: Secret key for JWT signing (required)
- `GOOGLE_CLIENT_ID`: Google OAuth2 client ID (Phase 3)
- `GOOGLE_CLIENT_SECRET`: Google OAuth2 client secret (Phase 3)

## Testing Commands

```bash
# Run tests
./mvnw test

# Build project
./mvnw clean install

# Run application
./mvnw spring-boot:run
```
````

## Implementation Checklist

### Before Moving to Phase 1

- [ ] Fix Role enum typo (ADIMIN → ADMIN)
- [ ] Fix AuthProvider enum typo (GOOSE → GOOGLE)
- [ ] Add JWT dependencies to pom.xml
- [ ] Add OAuth2 client dependency to pom.xml
- [ ] Create package structure
- [ ] Add JWT configuration to application.yaml
- [ ] Verify project compiles successfully
- [ ] Run tests to ensure no regressions

## Validation Steps

1. **Compile Check**: Ensure project compiles without errors

   ```bash
   ./mvnw compile
   ```

2. **Test Check**: Run existing tests

   ```bash
   ./mvnw test
   ```

3. **Dependency Check**: Verify new dependencies are resolved

   ```bash
   ./mvnw dependency:tree | grep -E "(jjwt|oauth2)"
   ```

## Common Issues

### JWT Dependency Conflicts

If you encounter dependency conflicts with Jackson, ensure you're using compatible versions. The provided JWT version (0.11.5) is compatible with Spring Boot 3.x.

### Enum Typos

When fixing enum typos, remember to:

1. Update any database records that reference the old enum values
2. Update any hardcoded strings in tests or configuration
3. Check for any serialization/deserialization issues

## Next Phase

After completing Phase 0, you'll be ready to implement:

1. Spring Security configuration
2. JWT service implementation
3. User repository setup
4. Basic authentication filters

## Time Estimate

Phase 0 should take approximately 30-45 minutes to complete, including:

- 10 minutes for fixing typos
- 15 minutes for updating dependencies
- 10 minutes for creating package structure
- 10 minutes for testing and validation
