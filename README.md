# task-api-spring-boot

Simple REST API for personal task management with stateless JWT authentication.

Built with **Spring Boot 3.5.11 + Java 21**, PostgreSQL 16, Spring Security 6, and Testcontainers. Virtual threads (Project Loom) enabled.

## Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.8+ (or use `./mvnw`)

## Quick Start

```bash
# Start PostgreSQL
docker compose up -d

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Access at: http://localhost:8080

## Documentation

- **API Docs**: [Project Overview](./docs/project-overview-pdr.md) | [Code Standards](./docs/code-standards.md)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **System Architecture**: [System Architecture](./docs/system-architecture.md)

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login, returns JWT |
| GET | `/api/v1/tasks` | Bearer JWT | List own tasks (paginated, sortable by createdAt DESC) |
| POST | `/api/v1/tasks` | Bearer JWT | Create a task |
| GET | `/api/v1/tasks/{id}` | Bearer JWT | Get a task (ownership required) |
| PUT | `/api/v1/tasks/{id}` | Bearer JWT | Update all task fields |
| DELETE | `/api/v1/tasks/{id}` | Bearer JWT | Delete a task |
| PATCH | `/api/v1/tasks/{id}/status` | Bearer JWT | Update status only |

## Testing

```bash
./mvnw test
```

Tests use **Testcontainers 1.20.4** with PostgreSQL 16. Docker must be running. Test profiles: `test` (H2 in-memory), `integration` (PostgreSQL via Testcontainers).

## Configuration

| Property | Env Var | Default | Purpose |
|---|---|---|---|
| JWT Secret | `JWT_SECRET` | `changeme-dev-secret-at-least-256-bits-long-here` | HMAC-SHA256 signing key (min 32 chars in production) |
| JWT Expiry | `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in milliseconds (24 hours) |
| Database URL | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/taskdb` | PostgreSQL connection |

## Tech Stack

| Component | Technology |
|---|---|
| Framework | Spring Boot 3.5.11 |
| Language | Java 21 |
| Database | PostgreSQL 16 + Spring Data JPA |
| Schema | Liquibase 4.29 (currently disabled, Hibernate manages via create-drop) |
| Security | Spring Security 6 + JWT (JJWT 0.12.6) |
| API Docs | Springdoc OpenAPI 2.8.6 |
| Testing | JUnit 5, Mockito, Testcontainers 1.20.4, H2 (test) |
| Code Gen | Lombok |
| Async | Virtual threads enabled (Java 21 Project Loom) |

## Project Structure

See [Codebase Summary](./docs/codebase-summary.md) for detailed package and file inventory.

Key directories:
- `src/main/java/com/eric6166/taskapi/` — Application code (7 packages, 21 classes)
- `src/test/java/com/eric6166/taskapi/` — Tests (5 test suites, 400+ test cases)
- `src/main/resources/db/changelog/` — Liquibase migrations

## Security

- **Authentication**: Stateless JWT only (no sessions, CSRF disabled)
- **Passwords**: BCrypt-hashed, never stored plaintext
- **Data Isolation**: Users can only access their own tasks
- **Error Responses**: Sensitive info hidden; 404 returned for unauthorized access

## Docker

PostgreSQL runs in Docker Compose with health checks and persistent volume.

```bash
docker compose up -d    # Start
docker compose down -v  # Stop + remove volumes
```

## CI/CD

GitHub Actions runs on all commits: lint, compile, test, build.

See `.github/workflows/` for CI configuration.
