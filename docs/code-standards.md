# Code Standards & Best Practices

## Package Organization

| Package | Contains | Enforcement |
|---|---|---|
| `config` | `@Configuration`, `@Bean` declarations | No business logic; configuration only |
| `controller` | `@RestController`, `@RequestMapping` | HTTP I/O + input validation only; all logic → service |
| `dto` | Java records (immutable request/response) | One record per endpoint use-case |
| `entity` | JPA `@Entity`, lifecycle callbacks | No service/repository calls; pure data model |
| `exception` | Custom exceptions, `@RestControllerAdvice` | Centralized; one handler class |
| `repository` | Spring Data `JpaRepository` interfaces | Query methods only; no business logic |
| `security` | JWT, Spring Security filters/providers | Auth/token handling; no business logic |
| `service` | Business logic, transaction boundary | All mutations transactional; queries may not be |

## Naming Conventions

### Classes & Interfaces
- **Entities**: PascalCase (e.g., `User`, `Task`, `Task Priority`)
- **DTOs**: `<Verb><Noun><Request|Response>` (e.g., `CreateTaskRequest`, `UpdateTaskStatusRequest`, `TaskResponse`)
- **Services**: `<Noun>Service` (e.g., `AuthService`, `TaskService`)
- **Controllers**: `<Noun>Controller` (e.g., `AuthController`, `TaskController`)
- **Custom Exceptions**: `<Context>Exception` (e.g., `ResourceNotFoundException`, `ConflictException`)
- **Filters**: `<Purpose>Filter` (e.g., `JwtAuthenticationFilter`)

### Constants & Enums
- **Enum Classes**: PascalCase (e.g., `TaskStatus`, `TaskPriority`)
- **Enum Values**: UPPER_SNAKE_CASE (e.g., `IN_PROGRESS`, `MEDIUM`)
- **Constants**: UPPER_SNAKE_CASE, final static

## Input Validation

- Always use `@Valid` on `@RequestBody` controller parameters
- All constraints (min/max length, patterns, nullability) defined on DTO records using JSR-303/JSR-380:
  - `@NotBlank` — cannot be empty or whitespace
  - `@Email` — must be valid email format
  - `@Size(min=8, max=255)` — password length
  - `@NotNull` — required field
- Never validate inside service layer — controller is the boundary
- MethodArgumentNotValidException caught by GlobalExceptionHandler, returns 400 VALIDATION_ERROR

## Error Handling Architecture

### Exception Hierarchy
- `ResourceNotFoundException extends RuntimeException` → 404 NOT_FOUND
- `ConflictException extends RuntimeException` → 409 CONFLICT
- `BadCredentialsException` (Spring Security) → 401 UNAUTHORIZED
- `MethodArgumentNotValidException` → 400 VALIDATION_ERROR with field details
- Generic `Exception` → 500 INTERNAL_SERVER_ERROR (no stack traces in production)

### Key Rules
1. **Centralized Handling**: All exceptions caught in `GlobalExceptionHandler` (never in controller)
2. **Soft 404 Security**: Return 404 for both "not found" AND "not owned by user" — never expose resource existence
3. **No Existence Leakage**: TaskService.findOwnedTask() combines findById + ownership check → single 404
4. **Unauthenticated 401**: SecurityContext missing or token invalid → 401 JSON with "Unauthorized"
5. **Error Response**: Always return `ErrorResponse(code: String, message: String)` record

## Transaction Management

### Transactional Boundaries
- `@Transactional` required on all `@Service` methods that write (create, update, delete)
- Read-only queries (GET endpoints) — transactional not required but optional for consistency
- Never annotate controllers with `@Transactional`
- Default propagation: REQUIRED; default isolation: READ_COMMITTED

### Open-in-View Policy
- `spring.jpa.open-in-view: false` — enforced, no lazy loading outside @Transactional methods
- All task lists loaded within service method with eager join or explicit fetch
- Prevents N+1 queries and ensures data consistency

## Security Architecture

### Authentication (JWT)
- **Token Generation**: AuthService.login() calls JwtService.generateToken(userDetails)
  - Algorithm: HMAC-SHA256
  - Claims: username (sub), issued-at (iat), expiration (exp)
  - Secret source: JWT_SECRET env var (min 32 chars, no default in production)
  - Expiration: JWT_EXPIRATION_MS (default 86400000 ms = 24 hours)
- **Token Validation**: JwtAuthenticationFilter extracts Bearer token from Authorization header
  - Validates signature against JWT_SECRET
  - Checks expiration
  - Loads UserDetails from DB via UserDetailsServiceImpl
  - Sets UsernamePasswordAuthenticationToken in SecurityContext

### Authorization
- **Stateless Sessions**: SessionCreationPolicy.STATELESS — no cookie-based sessions
- **CSRF Disabled**: No CSRF token needed (JWT in Authorization header, not form)
- **Public Paths**: /api/v1/auth/**, /v3/api-docs/**, /swagger-ui/**, /actuator/health, /actuator/info
- **Protected Paths**: /api/v1/tasks/** — requires SecurityContext with authenticated principal
- **Resource Ownership**: TaskService.findOwnedTask(taskId, userId) ensures user only sees/modifies own tasks
- **Password Hashing**: BCryptPasswordEncoder with default strength 10 (configured in SecurityConfig)

### Error Disclosure
- **JSON 401**: `{"error": "Unauthorized"}` for missing/invalid tokens
- **Structured 4xx/5xx**: `{"code": "...", "message": "..."}` — never include stack traces

## JPA / Hibernate Best Practices

### Fetch Strategies
- **Default FetchType.LAZY** on all `@OneToMany` and `@ManyToOne` associations (User.tasks, Task.user)
- **No N+1 Queries**: Service methods load all required data in single transaction
- **Explicit Joins**: Use @EntityGraph or FETCH JOIN in JPQL if eager loading needed

### Enum Mapping
- `@Enumerated(EnumType.STRING)` — store enum name ("TODO", not 0)
- Reason: Database-portable, human-readable, safe for migrations

### Entity Lifecycle
- `@PrePersist` sets createdAt = now()
- `@PreUpdate` sets updatedAt = now()
- Never expose createdAt/updatedAt as input fields (set automatically)

### Column Constraints
- username: UNIQUE, NOT NULL, VARCHAR(255)
- password: NOT NULL, VARCHAR(255) (after BCrypt hashing)
- task.status: NOT NULL, VARCHAR(20), default 'TODO'
- task.priority: NOT NULL, VARCHAR(20), default 'MEDIUM'
- Indexes: (user_id), (status), (due_date) on tasks table

### Pagination
- Default page size: 20 items
- Max page size: 100 items (enforced in repository or controller)
- Default sort: createdAt DESC (most recent first)
- Type: Page<TaskResponse> not List<TaskResponse>

## Data Transfer Object (DTO) Patterns

### DTO Design
- **Records Only**: Immutable, compiler-generated equals/hashCode/toString
- **Request DTOs**: Only contain writable fields (never id, createdAt, updatedAt)
- **Response DTOs**: Contain id, timestamps (createdAt, updatedAt), all readable fields
- **Validation on Request DTOs**: All JSR-303 constraints
- **No Validation on Response DTOs**: Already validated data

### DTO-Entity Mapping
- Manual mapping in service methods (no MapStruct/ModelMapper overhead)
- EntityManager.getReference() to avoid unnecessary SELECT in relationships
- Example: TaskResponse maps all Task fields except relationships

## Code Organization

### File Placement Rules
- One public class per .java file
- Static utility methods in dedicated utility classes (e.g., TestDataBuilder in test package)
- Constants in interfaces or dedicated Constants class if shared

### Method Size
- Target 30-50 lines per method
- Extract sub-methods at 60+ lines
- Test coverage enforced via CI/CD

## Documentation

### Code Comments
- Comment complex logic, not obvious code
- Document public API method purpose in JavaDoc
- Flag security-sensitive code with `// SECURITY:` comments

### Commit Messages
- Conventional commits: feat:, fix:, docs:, test:, refactor:, chore:
- No AI references in messages
- Link to issue IDs when applicable

## Testing Conventions

### Test Layers
1. **Unit Tests** (AuthServiceTest, TaskServiceTest): Mockito, no Spring context
2. **Controller Tests** (AuthControllerTest, TaskControllerTest): MockMvc, @MockitoBean
3. **Integration Tests** (TaskApiIntegrationTest): TestRestTemplate, Testcontainers PostgreSQL
4. **Smoke Test** (TaskApiApplicationTests): Context load verification

### Test Profiles
- `test`: H2 in-memory database, fast feedback
- `integration`: PostgreSQL via Testcontainers, realistic data

### Test Fixtures
- Use TestDataBuilder static factories: aUser(), aTask()
- Never hardcode test data; parameterize

## Virtual Threads (Java 21)

- Enabled via spring.threads.virtual.enabled: true
- Framework: Project Loom
- Use case: I/O-bound servlet handling (database, HTTP calls)
- No explicit code changes needed; transparent to application logic

## Dependency Injection

- Constructor-based DI via Lombok @RequiredArgsConstructor
- Never use field injection (@Autowired on fields)
- Reason: Immutability, testability, clear dependencies

## Production Readiness Checklist

- [ ] JWT_SECRET configured (min 32 chars, strong random)
- [ ] spring.jpa.hibernate.ddl-auto = validate
- [ ] spring.jpa.open-in-view = false
- [ ] Logging configured (INFO in production, DEBUG in dev)
- [ ] Error responses don't leak stack traces
- [ ] All custom exceptions handled in GlobalExceptionHandler
- [ ] Database indexes present (user_id, status, due_date)
- [ ] Connection pool configured (HikariCP)
- [ ] Liquibase applied (migrations versioned)
- [ ] Tests passing with 100% happy path coverage
