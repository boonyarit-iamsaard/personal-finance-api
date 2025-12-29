# AGENTS.md

## Development Commands

### Build & Run

- `./mvnw spring-boot:run` - Start application (port 8080)
- `./mvnw clean install` - Build project with tests
- `./mvnw clean package -DskipTests` - Build without tests

### Testing

- `./mvnw test` - Run all tests
- `./mvnw test -Dtest=ClassName` - Run specific test class (e.g., `./mvnw test -Dtest=AuthenticationServiceTest`)
- `./mvnw test -Dtest=ClassName#methodName` - Run a single test method
- `./mvnw test jacoco:report` - Run tests and generate coverage report (target/site/jacoco/index.html)
- **Always run tests** after modifying code to ensure no regressions.

### Database

- `docker compose up -d` - Start PostgreSQL + Adminer
- `docker compose logs -f postgres` - View database logs
- `docker compose down` - Stop services

### Code Quality

- `markdownlint **/*.md` - Check Markdown files
- `markdownlint --fix **/*.md` - Auto-fix Markdown issues
- `npx prettier --check **/*.md` - Check Markdown formatting
- `npx prettier --write **/*.md` - Format Markdown files

## Project Architecture

### Layered Architecture

The application follows a standard Spring Boot layered architecture:

- **Controller Layer** (`controller`): Handles HTTP requests, validation (`@Valid`), and maps responses. Strictly delegates business logic to Services.
- **Service Layer** (`service`): Contains business logic, transaction management (`@Transactional`), and communicates with Repositories.
- **Repository Layer** (`repository`): Interfaces extending `JpaRepository` for database access.
- **Domain Layer** (`entity`): JPA entities representing database tables.
- **DTO Layer** (`dto`): Records for data transfer, separating internal entities from API contracts.

### Key Technologies

- **Spring Boot 3.5.8**: Core framework.
- **Java 21**: Usage of records, switch expressions, and `var` is encouraged.
- **PostgreSQL**: Primary database.
- **Lombok**: Reduces boilerplate (`@Data`, `@RequiredArgsConstructor`, `@Builder`).
- **MapStruct**: Type-safe bean mapping between Entities and DTOs.
- **Spring Security + JWT**: Stateless authentication.

## Code Style & Conventions

### Java Guidelines

- **Indentation**: 4 spaces.
- **Line Length**: 120 characters.
- **Imports**: Specific order defined in `build/eclipse-formatter-config.importorder`. Avoid `.*` imports.
- **Null Safety**: Use `Optional<T>` for return types that might be empty. Avoid passing `null` as arguments.
- **Injection**: Always use Constructor Injection via `@RequiredArgsConstructor`. Avoid `@Autowired` on fields.
- **Immutability**: Prefer `final` fields and immutable objects (Records) where possible.
- **Entities**: Extend `BaseEntity`. Use `@Getter`, `@Setter` (avoid `@Data`). Use `1000` as initial sequence value.
- **DTOs**: Use Java Records. Suffix with `Request` or `Response`.

### Naming Conventions

- **Classes**: PascalCase (e.g., `TransactionService`).
- **Interfaces**: PascalCase (e.g., `TransactionRepository`).
- **Methods**: camelCase (e.g., `calculateTotalBalance`).
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`).
- **Tests**: `ClassName` + `Test`. Methods should be descriptive (e.g. `register_ShouldCreateUser_WhenEmailUnique`).

### Error Handling

- Use the global exception handling mechanism (`GlobalExceptionHandler`).
- Throw custom exceptions from `me.boonyarit.finance.exception` rather than generic `RuntimeException`.
- Return proper HTTP status codes via `ErrorResponse` DTO.

### Testing Guidelines

- **Unit Tests**: Use JUnit 5 and Mockito (`@ExtendWith(MockitoExtension.class)`).
- **Naming**: `methodName_ShouldExpectedBehavior_WhenCondition` (e.g. `authenticate_ShouldThrowException_WhenCredentialsInvalid`).
- **Structure**: Arrange-Act-Assert. Use `verify()` to check side effects.
- **Mocking**: Mock external dependencies (Repositories, Mappers).

## Common Tasks for Agents

### Adding a New Feature

1. **Define Entity**: Create the entity in `entity` package extending `BaseEntity`.
2. **Repository**: Create an interface in `repository` extending `JpaRepository`.
3. **DTOs**: Define `Request` and `Response` records in `dto` package.
4. **Mapper**: Create/Update a MapStruct mapper interface in `mapper`.
5. **Service**: Implement business logic in `service`, returning DTOs or Entities as appropriate.
6. **Controller**: Expose endpoints in `controller`, using DTOs for input/output.
7. **Tests**: Add unit tests for Service and integration tests for Controller.

### Database Migrations

- Currently using Hibernate `ddl-auto: update` for development.
- For production-ready changes, ensure entity mappings match the expected schema precisely.

### Working with Authentication

- `AuthenticationService` handles login/register.
- Use `@CurrentUser` annotation in controllers to inject the authenticated user.
- Security context is managed via JWT tokens.

## Environment Variables

- Copy `.env.example` to `.env` and configure your environment variables.
- Required: `JWT_SECRET` (generate with `openssl rand -base64 32`).
- Optional: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`.
- **Automatic loading**: Spring Boot automatically loads `.env` from project root.
- **Never commit `.env` file**.

## Markdown Requirements

- All fenced code blocks must specify language (e.g., `java`, `yaml`, `text`).
- Tables must be properly formatted with aligned pipes.
