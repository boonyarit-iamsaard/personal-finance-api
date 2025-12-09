# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot-based REST API for personal finance management built with Java 21 and PostgreSQL. The project follows a standard Maven structure with entity-driven design using Spring Data JPA.

## Common Development Commands

### Building and Running

```bash
# Run the application (starts on port 8080)
./mvnw spring-boot:run

# Build the project
./mvnw clean install

# Build without running tests
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ClassName

# Run tests with coverage
./mvnw test jacoco:report
```

### Code Formatting (Required before commits)

```bash
# Apply code formatting
./mvnw spotless:apply

# Check if code is properly formatted
./mvnw spotless:check

# Format and build in one command
./mvnw spotless:apply compile
```

### Database Setup

```bash
# Start PostgreSQL and Adminer with Docker Compose
docker compose up -d

# View database logs
docker compose logs -f postgres

# Stop services
docker compose down
```

## Architecture Overview

### Technology Stack

- **Framework**: Spring Boot 3.5.8
- **Language**: Java 21 (with modern language features)
- **Database**: PostgreSQL 15
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security
- **Build Tool**: Maven 3.9.11
- **Code Quality**: Spotless with Eclipse formatter

### Package Structure

```
me.boonyarit.finance/
├── Application.java           # Spring Boot entry point
├── entity/                   # JPA entities
│   ├── BaseEntity.java      # Abstract base with ID and audit fields
│   └── UserEntity.java      # User entity implementing UserDetails
└── enumeration/             # Enum definitions
    ├── Role.java            # User roles (ADIMIN, USER) - Note: typo exists
    └── AuthProvider.java    # Authentication providers (LOCAL, GOOGLE)
```

### Database Configuration

- **Database**: PostgreSQL on localhost:5432
- **Name**: personal-finance
- **Default credentials**: postgres/postgres (override with POSTGRES_USER and POSTGRES_PASSWORD env vars)
- **Schema generation**: Hibernate DDL auto-update enabled
- **Database admin**: Adminer available at http://localhost:8090

### Security Implementation

- UserEntity implements Spring Security's UserDetails interface
- Support for multiple authentication providers (local and Google OAuth)
- Role-based access control with ADMIN and USER roles

## Development Guidelines

### Code Style

- Uses Eclipse formatter configuration from `build/eclipse-formatter-config.xml`
- Import order defined in `build/eclipse-formatter-config.importorder`
- 4-space indentation, 120-character line limit
- Automatic formatting enforced by Spotless plugin

### Markdown Formatting

The project maintains high standards for Markdown documentation:

#### Linting and Formatting

```bash
# Check Markdown files for issues using markdownlint-cli
markdownlint specs/

# Check all Markdown files in the project
markdownlint **/*.md

# Check and fix some issues automatically
markdownlint --fix specs/

# Check if all Markdown files are properly formatted
npx prettier --check **/*.md

# Format all Markdown files (automatically fixes most issues)
npx prettier --write **/*.md

# Format only files in specs directory
npx prettier --write specs/
```

#### Markdown Linting Tool

This project uses [markdownlint-cli](https://github.com/igorshubovych/markdownlint-cli) for Markdown linting:

- **Installation**: `npm install -g markdownlint-cli`
- **Purpose**: Validates Markdown files against a set of rules to ensure consistency
- **Configuration**: Uses default rules with custom enforcement for:
  - Fenced code block language specification
  - Table formatting standards
  - Heading and list spacing requirements
- **Integration**: Can be integrated into CI/CD pipelines for automated checking

#### Markdown Requirements

- All fenced code blocks must specify language (e.g., `java, `yaml, ```text)
- Tables must be properly formatted with aligned pipes
- No trailing punctuation in headings
- Headings and lists should be surrounded by blank lines
- Consistent formatting using Prettier

#### Documentation Structure

- `specs/` directory contains detailed implementation specifications
- Each specification includes clear task breakdowns, implementation steps, and validation procedures
- All documentation follows markdownlint standards for readability and consistency

### Entity Design Pattern

All entities extend BaseEntity which provides:

- Long ID with sequence starting at 1000
- createdAt and updatedAt timestamps
- Lombok annotations for boilerplate reduction

### Testing Strategy

- Uses Spring Boot Test framework
- Spring Security Test for security testing
- Test classes mirror the main package structure

## Important Configuration Notes

### Environment Variables

- `POSTGRES_USER`: PostgreSQL username (default: postgres)
- `POSTGRES_PASSWORD`: PostgreSQL password (default: postgres)

### Application Properties

- Server runs on default port 8080
- JPA open-in-view disabled for performance
- SQL logging enabled in development
- Hibernate DDL auto-update enabled (consider changing to validate in production)

### Known Issues

- Role enum contains typo: "ADIMIN" instead of "ADMIN" in `src/main/java/me/boonyarit/finance/enumeration/Role.java`

## IDE Integration

- VS Code configuration in `.vscode/settings.json`
- IntelliJ IDEA configuration in `.idea/`
- Both IDEs configured to use Eclipse formatter for consistency
