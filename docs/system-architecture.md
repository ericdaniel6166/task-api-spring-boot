# System Architecture

## Overall Architecture Pattern

**Layered + Service-Oriented** with stateless JWT authentication.

```
┌─────────────────────────────────────────────────────────────────┐
│                      HTTP/REST Layer                            │
│         (Clients: Web Browser, Mobile App, API Client)          │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP Request (JSON)
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│         Spring Security & Authentication Layer                  │
│  JwtAuthenticationFilter → Extract Bearer Token → Validate      │
│  UserDetailsServiceImpl → Load user from DB                      │
│  SecurityContext → Set authenticated principal                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Authorized request
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                  Controller Layer (HTTP Boundary)               │
│   AuthController (register, login)                              │
│   TaskController (CRUD operations on /api/v1/tasks)             │
│   Input validation (@Valid on request DTOs)                     │
│   HTTP status codes (201, 200, 204, 404, 409, 401)             │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Business logic request
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                   Service Layer (Business Logic)                │
│   AuthService:                                                  │
│     - register(): password hashing (BCrypt), duplicate check    │
│     - login(): credential validation, JWT generation           │
│   TaskService:                                                  │
│     - CRUD operations scoped to currentUserId()                │
│     - Ownership check (findOwnedTask pattern)                  │
│     - Pagination + filtering logic                             │
│   All write operations: @Transactional                          │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Query/persist request
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│            Persistence Layer (Spring Data JPA)                  │
│   UserRepository (findByUsername, existsByUsername)             │
│   TaskRepository (findByUserId, findByUserIdAndStatus)          │
│   Hibernate ORM → SQL generation                                │
│   Connection pooling (HikariCP)                                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │ SQL
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                   PostgreSQL 16 (Database)                      │
│   users table (id, username, password, role, created_at)        │
│   tasks table (id, title, description, status, priority,        │
│                due_date, user_id, created_at, updated_at)       │
│   Indexes: (user_id), (status), (due_date)                      │
└──────────────────────────────────────────────────────────────────┘
```

## Request Flow (Annotated)

### Authentication & Authorization Flow

```
HTTP Request (with/without Authorization header)
    │
    ▼
Spring Security FilterChain
    │
    ├─→ CorsFilter (none configured, single-origin)
    │
    ├─→ JwtAuthenticationFilter (custom, runs before UsernamePasswordAuthenticationFilter)
    │   │
    │   ├─ No "Authorization: Bearer <token>" header?
    │   │   └─→ pass through with empty SecurityContext (anonymous)
    │   │
    │   └─ Has Bearer token?
    │       ├─ Extract token from "Authorization: Bearer <token>"
    │       ├─ JwtService.validateToken(token)? [signature + expiry + not-null]
    │       ├─ Extract username claim from token
    │       ├─ UserDetailsServiceImpl.loadUserByUsername(username)
    │       │  └─→ Query: UserRepository.findByUsername() → UserDetails
    │       ├─ Create UsernamePasswordAuthenticationToken(userDetails, null, authorities)
    │       └─ SecurityContextHolder.getContext().setAuthentication(token)
    │
    └─→ SecurityConfig.authorizeHttpRequests()
        │
        ├─ Request path in PUBLIC_PATHS?
        │  ├─ /api/v1/auth/** (register, login)
        │  ├─ /v3/api-docs/**, /swagger-ui/** (API docs)
        │  ├─ /actuator/health, /actuator/info (health checks)
        │  └─ → permitAll() [no SecurityContext needed]
        │
        └─ Other paths (/api/v1/tasks**)
           └─ → authenticated() required [401 if SecurityContext.getAuthentication() is null]
```

### Task Operations Flow (with Ownership Validation)

```
GET /api/v1/tasks/123
    │
    ▼
TaskController.getTask(id=123)
    │
    ▼
TaskService.getTask(123)
    │
    ├─ Get currentUserId() from SecurityContext
    │  └─ SecurityContextHolder.getContext().getAuthentication().getName()
    │     → UserRepository.findByUsername() → User.id
    │
    ├─ findOwnedTask(123, userId)
    │  ├─ TaskRepository.findById(123)
    │  ├─ Check: task.getUser().getId() == userId?
    │  └─ NO → throw ResourceNotFoundException (404)
    │  └─ YES → return task
    │
    ▼
Return TaskResponse DTO (never return entity)
```

## JWT Token Lifecycle

```
Registration Flow:
  POST /api/v1/auth/register { username, password }
      │
      ├─ AuthService.register()
      │  ├─ UserRepository.existsByUsername()? → YES: throw ConflictException (409)
      │  ├─ BCryptPasswordEncoder.encode(password)
      │  ├─ User entity created + saved to DB
      │  └─ Return RegisterResponse (id, username, createdAt)
      │
      └─ No JWT issued on registration

Login Flow:
  POST /api/v1/auth/login { username, password }
      │
      ├─ AuthService.login()
      │  ├─ AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)
      │  │  └─ DaoAuthenticationProvider.authenticate()
      │  │     ├─ UserDetailsServiceImpl.loadUserByUsername(username)
      │  │     ├─ BCryptPasswordEncoder.matches(password, hashedPassword)?
      │  │     └─ NO: throw BadCredentialsException (401)
      │  │
      │  ├─ YES: JwtService.generateToken(userDetails)
      │  │  ├─ Create Jwts.builder()
      │  │  ├─ setClaims(): { sub: username }
      │  │  ├─ setIssuedAt(now)
      │  │  ├─ setExpiration(now + JWT_EXPIRATION_MS)
      │  │  ├─ signWith(ALGORITHM.HMAC_SHA256, JWT_SECRET)
      │  │  └─ compact() → signed token string
      │  │
      │  └─ Return AuthResponse { token, expiresIn }
      │
      └─ Client stores token (localStorage, sessionStorage, or cookie)

Subsequent Requests with JWT:
  GET /api/v1/tasks Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
      │
      ├─ JwtAuthenticationFilter.doFilterInternal()
      │  ├─ Extract "Authorization" header
      │  ├─ Remove "Bearer " prefix
      │  ├─ JwtService.validateToken(token)
      │  │  ├─ JwtParser.parseSignedClaims(token, JWT_SECRET)
      │  │  │  └─ Verify HMAC-SHA256 signature with JWT_SECRET
      │  │  ├─ Check expiration claim (exp > now)?
      │  │  └─ NO: return false / throw ExpiredJwtException
      │  │
      │  ├─ JwtService.extractUsername(token) → "john_doe"
      │  ├─ UserDetailsServiceImpl.loadUserByUsername("john_doe")
      │  ├─ Set SecurityContext with authenticated principal
      │  │
      │  └─ Continue to controller
      │
      └─ Controller/Service access SecurityContextHolder.getContext().getAuthentication().getName()
         → "john_doe" available throughout request scope
```

## Database Schema & Relationships

```
users (id: BIGSERIAL PRIMARY KEY)
├─ id: BIGSERIAL (auto-increment)
├─ username: VARCHAR(255) NOT NULL UNIQUE
├─ password: VARCHAR(255) NOT NULL [BCrypt-hashed ~60 chars]
├─ role: VARCHAR(50) DEFAULT 'ROLE_USER'
├─ created_at: TIMESTAMPTZ NOT NULL DEFAULT now()
│
└─ Relation: @OneToMany(mappedBy="user")
   └─ List<Task> tasks (LAZY loading)

tasks (id: BIGSERIAL PRIMARY KEY, user_id: BIGINT FOREIGN KEY)
├─ id: BIGSERIAL (auto-increment)
├─ title: VARCHAR(255) NOT NULL
├─ description: TEXT (nullable)
├─ status: VARCHAR(20) NOT NULL DEFAULT 'TODO' [EnumType.STRING]
│  └─ Allowed: TODO | IN_PROGRESS | DONE
├─ priority: VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' [EnumType.STRING]
│  └─ Allowed: LOW | MEDIUM | HIGH
├─ due_date: DATE (nullable)
├─ user_id: BIGINT NOT NULL [FK → users.id, ON DELETE CASCADE]
├─ created_at: TIMESTAMPTZ NOT NULL [@PrePersist sets this]
├─ updated_at: TIMESTAMPTZ NOT NULL [@PreUpdate sets this]
│
└─ Indexes:
   ├─ idx_tasks_user_id [performance for findByUserId queries]
   ├─ idx_tasks_status [filter tasks by status]
   └─ idx_tasks_due_date [sort/filter by due date]

One-to-Many: User.tasks (LAZY) → Task.user (EAGER implicit)
Cascade: User deletion cascades to tasks (database constraint)
```

## Package & Responsibility Map

```
config/                     Configuration & Infrastructure Setup
├─ AppProperties           JWT config binding (secret, expirationMs)
├─ OpenApiConfig           Springdoc metadata, Swagger UI title/version
└─ SecurityConfig          Filter chain, auth providers, CORS (disabled)

security/                   Authentication & Token Management
├─ JwtService             Token generation/validation/parsing (stateless)
├─ JwtAuthenticationFilter Custom filter for Bearer token extraction
└─ UserDetailsServiceImpl  Spring Security user loader from DB

entity/                     Domain Model (JPA)
├─ User                    User aggregate (owns tasks)
├─ Task                    Task entity (belongs to User)
├─ TaskStatus             Enum: TODO, IN_PROGRESS, DONE
└─ TaskPriority           Enum: LOW, MEDIUM, HIGH

repository/                Data Access Layer (Spring Data JPA)
├─ UserRepository         findByUsername, existsByUsername
└─ TaskRepository         findByUserId, findByUserIdAndStatus

service/                    Business Logic & Transactions
├─ AuthService            register (no JWT), login (JWT issue)
└─ TaskService            CRUD with ownership checks, currentUserId resolution

controller/                REST Endpoints (HTTP Boundary)
├─ AuthController         /api/v1/auth/* (register, login)
└─ TaskController         /api/v1/tasks/* (CRUD + status PATCH)

dto/                        Data Transfer Objects (Request/Response)
├─ auth/*                 RegisterRequest, AuthResponse, LoginRequest, RegisterResponse
├─ task/*                 CreateTaskRequest, UpdateTaskRequest, TaskResponse, UpdateTaskStatusRequest
└─ ErrorResponse          Structured error (code, message)

exception/                  Error Handling
├─ GlobalExceptionHandler  Centralized @RestControllerAdvice (404/409/400/401/500)
├─ ResourceNotFoundException 404: "Task not found" or "not owned"
└─ ConflictException       409: "User already exists"
```

## Error Handling Architecture

```
Exception Thrown
    │
    ▼
GlobalExceptionHandler (@RestControllerAdvice)
    │
    ├─ ResourceNotFoundException
    │  └─ Response: 404 NOT_FOUND, { code: "NOT_FOUND", message: "..." }
    │
    ├─ ConflictException
    │  └─ Response: 409 CONFLICT, { code: "CONFLICT", message: "..." }
    │
    ├─ MethodArgumentNotValidException
    │  └─ Response: 400 VALIDATION_ERROR, { code: "VALIDATION_ERROR", message: "... (field errors)" }
    │
    ├─ HttpMessageNotReadableException
    │  └─ Response: 400 BAD_REQUEST, { code: "BAD_REQUEST", message: "..." }
    │
    ├─ BadCredentialsException (Spring Security)
    │  └─ Response: 401 UNAUTHORIZED, { code: "UNAUTHORIZED", message: "Invalid credentials" }
    │
    └─ Generic Exception
       └─ Response: 500 INTERNAL_SERVER_ERROR, { code: "INTERNAL_ERROR", message: "..." }
          (Stack trace hidden in production)
```

## Data Flow: Creating a Task

```
POST /api/v1/tasks
Authorization: Bearer <jwt>
Content-Type: application/json
{
  "title": "Buy milk",
  "description": "Organic milk",
  "priority": "HIGH",
  "dueDate": "2026-05-01"
}

    │
    ▼
TaskController.createTask(@Valid @RequestBody CreateTaskRequest)
    │
    ├─ Validation: title @NotBlank, priority @NotNull
    │  └─ Failed: → 400 VALIDATION_ERROR
    │
    ├─ SecurityContext has authenticated principal? (handled by filter)
    │  └─ NO: 401 UNAUTHORIZED (shouldn't reach here)
    │
    ▼
TaskService.createTask(request)
    │ @Transactional
    │
    ├─ currentUserId() → resolve from SecurityContext
    ├─ Load User entity by id [check: user owns this operation]
    │
    ├─ Create Task entity:
    │  ├─ title = request.title
    │  ├─ description = request.description
    │  ├─ priority = request.priority
    │  ├─ dueDate = request.dueDate
    │  ├─ status = TODO (default)
    │  ├─ user = User (from currentUserId)
    │  └─ @PrePersist sets createdAt = now()
    │
    ├─ taskRepository.save(task)
    │  └─ INSERT into tasks (title, description, ..., user_id, created_at)
    │
    └─ toResponse(savedTask) → TaskResponse (id, timestamps, all fields)

    │
    ▼
HTTP Response: 201 CREATED
{
  "id": 42,
  "title": "Buy milk",
  "description": "Organic milk",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-05-01",
  "createdAt": "2026-04-24T10:30:00Z",
  "updatedAt": "2026-04-24T10:30:00Z"
}
```

## Deployment Topology

```
Client (Browser/API Client)
    │ HTTPS
    ▼
Load Balancer (optional, not in scope)
    │
    ▼
Docker Container (task-api)
├─ JVM (Java 21)
├─ Spring Boot 3.5.11 (embedded Tomcat, port 8080)
│  ├─ Security filter chain
│  ├─ Controllers, Services, Repositories
│  └─ Virtual threads enabled
│
└─ Network
    │
    ▼
PostgreSQL 16 Container (docker-compose)
├─ Database: taskdb
├─ User: taskuser
├─ Port: 5432
└─ Volume: postgres_data (persistent)

Notes:
- Single JVM instance (no clustering configured)
- No session replication (stateless JWT)
- Database migrations via Liquibase (currently disabled, Hibernate create-drop)
- Connection pooling: HikariCP (default, configured in Spring Boot)
```

## Technology Decisions

| Decision | Rationale |
|----------|-----------|
| JWT over Sessions | Stateless, scalable, no server-side state |
| HMAC-SHA256 | Fast, symmetric, suitable for single-service architecture |
| BCrypt for passwords | Industry standard, automatic salt + iteration count |
| Spring Data JPA | Standard ORM for Spring Boot, minimal boilerplate |
| Liquibase disabled | Hibernate create-drop sufficient for dev; for production, enable Liquibase |
| Records for DTOs | Immutable, compiler-generated equals/hashCode, Java 15+ standard |
| @Transactional on service | Business logic boundary, declarative transaction management |
| LAZY loading | Prevents N+1 queries, open-in-view: false enforced |
| EnumType.STRING | Database-portable, human-readable, safe migrations |
| Virtual threads | Java 21 Project Loom, I/O-bound servlet handling |
