# Personal Finance API

A Spring Boot REST API for personal finance management with JWT authentication.

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Setup

1. **Copy environment template:**

   ```bash
   cp .env.example .env
   ```

2. **Generate JWT secret:**

   ```bash
   openssl rand -base64 32
   # Add the output to .env as JWT_SECRET
   ```

3. **Start database:**

   ```bash
   docker compose up -d postgres
   ```

4. **Run application:**

   ```bash
   ./mvnw spring-boot:run
   ```

The application will be available at <http://localhost:8080>

## Environment Variables

Required environment variables (configure in `.env`):

- `JWT_SECRET` - JWT token signing secret

Optional environment variables:

- `POSTGRES_USER` - Database username (default: postgres)
- `POSTGRES_PASSWORD` - Database password (default: postgres)
- `POSTGRES_DB` - Database name (default: personal-finance)

## API Documentation

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI spec: <http://localhost:8080/v3/api-docs>

## Development

See [AGENTS.md](./AGENTS.md) for detailed development guidelines, testing, and code style information.
