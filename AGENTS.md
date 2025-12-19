# AGENTS.md

## Development Commands

### Build & Run

- `./mvnw spring-boot:run` - Start application (port 8080)
- `./mvnw clean install` - Build project with tests
- `./mvnw clean package -DskipTests` - Build without tests

### Testing

- `./mvnw test` - Run all tests
- `./mvnw test -Dtest=ClassName` - Run specific test class
- `./mvnw test jacoco:report` - Run tests with coverage

### Database

- `docker compose up -d` - Start PostgreSQL + Adminer
- `docker compose logs -f postgres` - View database logs
- `docker compose down` - Stop services

### Code Quality

- `markdownlint specs/` - Check Markdown files
- `markdownlint --fix specs/` - Auto-fix Markdown issues
- `npx prettier --check **/*.md` - Check Markdown formatting
- `npx prettier --write **/*.md` - Format Markdown files

## Code Style Guidelines

### Formatting

- Uses Eclipse formatter with 4-space indentation, 120-char limit
- Import order defined in `build/eclipse-formatter-config.importorder`

### Package Structure

Follow `me.boonyarit.finance.*` with subpackages: entity, dto (request/response), service, repository, config, security, exception, enumeration, mapper.

### Naming Conventions

- Classes: PascalCase (UserEntity, AuthenticationService)
- Methods/variables: camelCase (existsByEmailIgnoreCase)
- Database columns: snake_case (first_name, created_at)

### Entity Pattern

All entities extend BaseEntity with @Id sequence starting from 1000, createdAt/updatedAt audit fields, and Lombok @Getter/@Setter.

### Spring Patterns

- Use @RequiredArgsConstructor for dependency injection
- Services annotated with @Service, @Transactional where needed
- Repositories extend JpaRepository with custom query methods
- DTOs use Java records for request/response objects

### Security

- UserEntity implements UserDetails
- Authorities: "ROLE\_" + enum.name() format
- Support for multiple AuthProvider (LOCAL, GOOGLE)

### Error Handling

- Custom exceptions in exception package
- GlobalExceptionHandler for REST error responses

### Dependencies

- Java 21 with modern language features
- Spring Boot 3.5.8
- PostgreSQL with JPA/Hibernate
- MapStruct for entity-DTO mapping
- JWT for authentication
- Lombok for boilerplate reduction

## Database Configuration

- PostgreSQL on localhost:5432, database name: personal-finance
- Default credentials: postgres/postgres (override with POSTGRES_USER and POSTGRES_PASSWORD env vars)
- Adminer available at <http://localhost:8090>
- Hibernate DDL auto-update enabled (consider changing to validate in production)

## Markdown Requirements

- All fenced code blocks must specify language (e.g., `java`, `yaml`, `text`)
- Tables must be properly formatted with aligned pipes
- No trailing punctuation in headings
- Headings and lists should be surrounded by blank lines
- Documentation in `specs/` directory with clear task breakdowns and validation procedures
