# Codebase Summary

**Stats**: 733 LOC (main) + 683 LOC (tests) | 21 classes + 5 test suites | Spring Boot 3.5.11, Java 21, PostgreSQL 16

## Application Structure (`src/main/java/com/eric6166/taskapi/`)

### Entry Point (1 file)
```
TaskApiApplication.java              ← @SpringBootApplication, @ConfigurationPropertiesScan
```

### Configuration (3 files, 119 LOC)
```
config/
  AppProperties.java                 ← @ConfigurationProperties("app.security.jwt") binding
  OpenApiConfig.java                 ← Springdoc OpenAPI metadata, BearerAuth scheme, title/description
  SecurityConfig.java                ← SecurityFilterChain (STATELESS), BCryptPasswordEncoder, AuthenticationManager, PUBLIC_PATHS array
```

### Security (3 files, 140 LOC)
```
security/
  JwtService.java (55 LOC)           ← generateToken(UserDetails) → signed JWT, validateToken(token) → boolean, extractUsername(token)
                                       Algorithm: HMAC-SHA256, claims: username (sub), exp, iat
  JwtAuthenticationFilter.java (55)  ← OncePerRequestFilter, extractBearerToken() from Authorization header
                                       Loads UserDetails, validates signature+expiry, sets SecurityContext
  UserDetailsServiceImpl.java (30)    ← loadUserByUsername(username) → Spring UserDetails from database
                                       Throws UsernameNotFoundException if not found
```

### Entities (4 files, 109 LOC)
```
entity/
  User.java (41 LOC)                 ← @Entity, PK: id (BIGSERIAL), username (UNIQUE VARCHAR 255), password (VARCHAR 255),
                                       role (VARCHAR 50, default "ROLE_USER"), createdAt (TIMESTAMPTZ)
                                       Relation: @OneToMany(mappedBy="user") List<Task> tasks (LAZY)
  Task.java (63)                     ← @Entity, PK: id (BIGSERIAL), title, description (TEXT), status, priority, dueDate (DATE),
                                       user_id (FK BIGINT), createdAt, updatedAt (TIMESTAMPTZ)
                                       @PrePersist sets createdAt, @PreUpdate sets updatedAt
                                       Indexes: idx_tasks_user_id, idx_tasks_status, idx_tasks_due_date
  TaskStatus.java (5)                ← Enum: TODO, IN_PROGRESS, DONE (stored as STRING)
  TaskPriority.java (5)              ← Enum: LOW, MEDIUM, HIGH (stored as STRING)
```

### Repositories (2 files, 27 LOC)
```
repository/
  UserRepository.java (12 LOC)       ← findByUsername(String) → Optional<User>, existsByUsername(String) → boolean
  TaskRepository.java (15)           ← findByUserId(Long, Pageable) → Page<Task>,
                                       findByUserIdAndStatus(Long, TaskStatus, Pageable) → Page<Task>
```

### DTOs (9 files, 44 LOC combined)
```
dto/
  ErrorResponse.java                 ← record(code: String, message: String)
  auth/
    RegisterRequest.java             ← record(username: @Email String, password: @Size(min=8) String)
    RegisterResponse.java            ← record(id: Long, username: String, createdAt: OffsetDateTime)
    LoginRequest.java                ← record(username: String, password: String)
    AuthResponse.java                ← record(token: String, expiresIn: Long) — milliseconds until expiry
  task/
    CreateTaskRequest.java           ← record(title: @NotBlank String, description: String, priority: @NotNull TaskPriority, dueDate: LocalDate)
    UpdateTaskRequest.java           ← record(title: @NotBlank String, description: String, priority: @NotNull TaskPriority, dueDate: LocalDate)
    UpdateTaskStatusRequest.java     ← record(status: @NotNull TaskStatus)
    TaskResponse.java                ← record(id: Long, title, description, status, priority, dueDate, createdAt, updatedAt)
```

### Services (2 files, 151 LOC)
```
service/
  AuthService.java (54 LOC)          ← register() → validates no duplicate, hashes password, saves User, returns RegisterResponse
                                       login() → AuthenticationManager.authenticate(), generates JWT via JwtService
  TaskService.java (97)              ← listTasks(status?, pageable) with ownership check via currentUserId()
                                       createTask() → maps request to Task, sets user, saves, returns TaskResponse
                                       getTask(id) → findOwnedTask() → 404 if not owned
                                       updateTask(id, request) → merge fields, save
                                       deleteTask(id) → findOwnedTask() → delete
                                       updateTaskStatus(id, request) → findOwnedTask() → update status only
                                       private currentUserId() → resolves from SecurityContext
```

### Controllers (2 files, 94 LOC)
```
controller/
  AuthController.java (30 LOC)       ← POST /api/v1/auth/register (@Valid RegisterRequest) → 201 RegisterResponse
                                       POST /api/v1/auth/login (@Valid LoginRequest) → 200 AuthResponse (token + expiresIn)
  TaskController.java (64)           ← GET /api/v1/tasks?status={status}&page=0&size=20&sort=createdAt,desc → Page<TaskResponse>
                                       POST /api/v1/tasks (@Valid CreateTaskRequest) → 201 TaskResponse
                                       GET /api/v1/tasks/{id} → TaskResponse (404 if not owned)
                                       PUT /api/v1/tasks/{id} (@Valid UpdateTaskRequest) → TaskResponse
                                       DELETE /api/v1/tasks/{id} → 204 NO_CONTENT
                                       PATCH /api/v1/tasks/{id}/status (@Valid UpdateTaskStatusRequest) → TaskResponse
                                       All task endpoints: @SecurityRequirement(name="BearerAuth")
```

### Exception Handling (3 files, 69 LOC)
```
exception/
  GlobalExceptionHandler.java (55)   ← @RestControllerAdvice handles:
                                       ResourceNotFoundException → 404 ErrorResponse
                                       ConflictException → 409 ErrorResponse
                                       MethodArgumentNotValidException → 400 VALIDATION_ERROR
                                       HttpMessageNotReadableException → 400 BAD_REQUEST
                                       BadCredentialsException → 401 UNAUTHORIZED
                                       Generic Exception → 500 INTERNAL_SERVER_ERROR (no stack trace)
  ResourceNotFoundException.java (7) ← extends RuntimeException, used for 404 (not found + not owned)
  ConflictException.java (7)         ← extends RuntimeException, used for 409 (duplicate resource)
```

## Configuration Resources (`src/main/resources/`)

```
application.yml                      ← Spring profiles, JPA (ddl-auto: create-drop, open-in-view: false),
                                       Liquibase (enabled: false), JWT props, Springdoc, logging (WARN), virtual threads
application-dev.yml                  ← Override: datasource (postgres://localhost:5432/taskdb), logging (DEBUG), lazy-init
db/changelog/
  db.changelog-master.xml            ← Liquibase master changelog (currently disabled)
  001-create-users-table.xml         ← CREATE TABLE users (id, username UNIQUE, password, role, created_at)
  002-create-tasks-table.xml         ← CREATE TABLE tasks (id, title, description, status, priority, due_date, user_id FK, created_at, updated_at)
                                       CREATE INDEX idx_tasks_user_id, idx_tasks_status, idx_tasks_due_date
```

## Test Structure (`src/test/java/com/eric6166/taskapi/`)

### Base & Integration (2 files, 181 LOC)
```
integration/
  AbstractIntegrationTest.java       ← @SpringBootTest, @Testcontainers, @Container PostgreSQL 16 testcontainer,
                                       environment setup (no profiles)
  TaskApiIntegrationTest.java (166)  ← E2E tests: auth flow (register + login), task CRUD, ownership 404,
                                       unauthenticated 401, pagination, status filter
```

### Controllers (2 files, 186 LOC)
```
controller/
  AuthControllerTest.java (83 LOC)   ← @SpringBootTest, MockMvc, tests register success/validation/conflict (409),
                                       login success/bad credentials (401)
  TaskControllerTest.java (103)      ← @SpringBootTest, MockMvc, @WithMockUser("testuser"), tests all 6 task endpoints,
                                       request validation, 404 not found
```

### Services (2 files, 249 LOC)
```
service/
  AuthServiceTest.java (113 LOC)     ← Mockito unit tests: register success/duplicate conflict,
                                       login success/not found/bad password (BadCredentialsException)
  TaskServiceTest.java (136)         ← Mockito unit tests: create, getTask with ownership check,
                                       delete, list (default + filtered), pagination
```

### Utilities (1 file)
```
util/
  TestDataBuilder.java               ← Static factory methods: aUser(), aTask() for test fixture creation
```

## Build & Infrastructure

```
pom.xml                              ← Spring Boot 3.5.11, Java 21, dependencies:
                                       - spring-boot-starter-web (embedded Tomcat)
                                       - spring-boot-starter-security (Spring Security 6)
                                       - spring-boot-starter-data-jpa (Hibernate)
                                       - postgresql 16+ driver
                                       - liquibase-core 4.29
                                       - jjwt 0.12.6 (JWT generation + validation)
                                       - springdoc-openapi 2.8.6 (Swagger UI auto-gen)
                                       - lombok (code generation)
                                       - junit-jupiter 5 (testing)
                                       - mockito (mocking)
                                       - testcontainers 1.20.4 (PostgreSQL container)
                                       - h2 (test DB, in-memory)
docker-compose.yml                   ← PostgreSQL 16-Alpine on port 5432, volume postgres_data, healthcheck
Makefile                             ← Targets: up (compose), down, run (boot), build (mvn), test, compile
.github/workflows/ci.yml             ← GitHub Actions: lint, compile, test, build on push
```

## API Summary

| Endpoint | Method | Auth | Returns | Validates |
|----------|--------|------|---------|-----------|
| `/api/v1/auth/register` | POST | Public | 201 RegisterResponse | @Valid RegisterRequest |
| `/api/v1/auth/login` | POST | Public | 200 AuthResponse | @Valid LoginRequest |
| `/api/v1/tasks` | GET | JWT | 200 Page<TaskResponse> | status filter (optional) |
| `/api/v1/tasks` | POST | JWT | 201 TaskResponse | @Valid CreateTaskRequest |
| `/api/v1/tasks/{id}` | GET | JWT | 200 TaskResponse | 404 if not owned |
| `/api/v1/tasks/{id}` | PUT | JWT | 200 TaskResponse | @Valid UpdateTaskRequest |
| `/api/v1/tasks/{id}` | DELETE | JWT | 204 NO_CONTENT | 404 if not owned |
| `/api/v1/tasks/{id}/status` | PATCH | JWT | 200 TaskResponse | @Valid UpdateTaskStatusRequest |

Public paths also include: `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`, `/actuator/info`
