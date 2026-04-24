# Project Overview & PDR — Task API

## Purpose

Production-ready REST API for personal task management with stateless JWT authentication. Users register, authenticate, and manage their own tasks via CRUD operations and status transitions. Built on Spring Boot 3.5.11 + Java 21 as a reference implementation of modern Spring Security 6, JWT auth (JJWT 0.12.6), and PostgreSQL 16 integration with virtual thread support.

## Core User Stories

| ID | As a user I want to... | So that... | Status |
|---|---|---|---|
| US-1 | Register with username + password | I can create an account | DONE |
| US-2 | Login and receive a JWT token | I can make authenticated requests | DONE |
| US-3 | Create tasks (title, description, priority, due date) | I can track my work items | DONE |
| US-4 | List my tasks with pagination and optional status filter | I can view relevant work | DONE |
| US-5 | Update a task's title, description, priority, or due date | I can revise task details | DONE |
| US-6 | Transition task status (TODO → IN_PROGRESS → DONE) | I can track progress | DONE |
| US-7 | Delete a task | I can remove completed/cancelled items | DONE |

## Product Requirements

### Functional Requirements
- **FR-1**: Users must register with unique username and password (min 8 chars)
- **FR-2**: Passwords stored as BCrypt hashes, never plaintext
- **FR-3**: Login returns JWT token with 24-hour expiration (configurable)
- **FR-4**: All task operations scoped to authenticated user (no cross-user access)
- **FR-5**: Tasks have: title, description, status (TODO/IN_PROGRESS/DONE), priority (LOW/MEDIUM/HIGH), due date, createdAt, updatedAt
- **FR-6**: Task list endpoint supports pagination (default 20 items/page, max 100) and optional status filtering
- **FR-7**: Deletion/update returns 404 for non-existent or non-owned tasks (no existence leakage)
- **FR-8**: All non-GET requests are transactional
- **FR-9**: API responses use DTO records (never return JPA entities directly)

### Non-Functional Requirements
- **NFR-1**: Stateless architecture — no server-side sessions, JWT only
- **NFR-2**: Database schema versioned via Liquibase 4.29 (currently disabled, Hibernate create-drop in dev)
- **NFR-3**: Centralized exception handling returning structured JSON error responses
- **NFR-4**: All endpoints with @Valid request body validation
- **NFR-5**: Virtual threads enabled via Java 21 Project Loom
- **NFR-6**: Open-in-view disabled (no lazy loading outside transactions)
- **NFR-7**: LAZY loading on all associations
- **NFR-8**: Soft security: return 404 for 403 scenarios (unauthorized access to others' tasks)
- **NFR-9**: CORS not required (single-origin, JWT in Authorization header)

### Security Requirements
- **SEC-1**: JWT secret configured via JWT_SECRET env var (min 32 chars production)
- **SEC-2**: CSRF disabled (stateless API, session-less)
- **SEC-3**: Passwords hashed with BCrypt (strength 10)
- **SEC-4**: Authentication filter validates token before controller access
- **SEC-5**: Authorization checks task ownership in TaskService.findOwnedTask()
- **SEC-6**: HTTP 401 for unauthenticated requests, 404 for unauthorized (not 403)
- **SEC-7**: No sensitive info in error responses (stack traces hidden in production)

### Performance Requirements
- **PERF-1**: Pagination max 100 items to prevent memory exhaustion
- **PERF-2**: Task queries indexed on: user_id, status, due_date
- **PERF-3**: Virtual threads enabled for I/O-bound operations
- **PERF-4**: No N+1 query issues (confirmed via integration tests)

## Out of Scope (Not Implementing)

- Refresh tokens / token revocation lists
- Multi-role authorization (all users are ROLE_USER)
- Email verification or password resets
- Real-time notifications (WebSocket, Server-Sent Events)
- File attachments or media uploads
- Task sharing / collaboration between users
- Rate limiting
- OAuth2 / third-party authentication
- Admin dashboard or user management

## Success Criteria

1. All 7 user stories implemented with passing unit + integration tests
2. Zero cross-user data leakage (confirmed via integration test)
3. All endpoints return proper HTTP status codes (201/200/204/404/409/500)
4. Pagination works with default sort (createdAt DESC)
5. JWT token generation and validation functional end-to-end
6. Swagger UI auto-generated from code (Springdoc OpenAPI 2.8.6)
7. Docker Compose local dev environment stable
8. CI/CD passing on GitHub Actions (lint, compile, test, build)
