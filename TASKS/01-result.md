# Session 1: Foundation, SQL, Auth, and Profile Result

## Completed Deliverables

### 1. Maven Project Configuration & Infrastructure
- **`backend/pom.xml`**: Configured Spring Boot 3.3.4 targeting Java 21 with dependencies for Spring Web, Spring JDBC (`JdbcTemplate`), Spring Validation, Spring Security Crypto (`BCryptPasswordEncoder`), H2 database, MySQL Connector/J, and test starters. Added `maven-surefire-plugin` configuration for JVM dynamic agent loading.
- **`backend/.gitignore`**: Added ignores for build targets, local H2 database files (`*.mv.db`, `data/`), and IDE configurations.
- **`backend/src/main/resources/application.properties`**: Default persistent H2 in MySQL compatibility mode (`jdbc:h2:file:./data/courserecdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;AUTO_SERVER=TRUE`) with automatic schema initialization.
- **`backend/src/main/resources/application-mysql.properties`**: Environment-driven MySQL profile using standard JDBC credentials and flags.

### 2. SQL Schema & Idempotent Database Initialization
- **`backend/src/main/resources/schema.sql`**: Created relational schema matching `API_CONTRACT.md`:
  - `users`: Stores user identity, password hash, role (`STUDENT`, `ADMIN`), academic background, proficiency level, and career goal.
  - `user_interests`: Child table with foreign key cascade delete.
  - `user_skills`: Child table with skill name and proficiency level, with cascade delete.
  - `courses`: Course catalog data, including category, difficulty level, duration, platform, URL, career goals, prerequisites, and demo flag.
  - `course_topics`: Child table for course topics with cascade delete.
  - `course_skills`: Child table for course skills with cascade delete.
  - `saved_courses`: User course bookmarks with unique constraint on `(user_id, course_id)` and cascade deletes.
  - `sessions`: Bearer session tokens storing SHA-256 token hash and expiration timestamp with cascade delete.
- **`backend/src/main/java/com/example/courserec/auth/DatabaseInitializer.java`**:
  - Seeds 14 varied demo courses across diverse categories (Web Dev, AI/ML, Cloud, DSA, Cybersecurity, Mobile, DevOps, DB Design, UI/UX, Distributed Systems, Big Data) with topics and skills idempotently (checks `COUNT(*) FROM courses WHERE is_demo = true`).
  - Supports optional admin bootstrap when `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set; never hardcodes default credentials.

### 3. Authentication & Security
- **`backend/src/main/java/com/example/courserec/auth/SecurityConfig.java`**: Configured `BCryptPasswordEncoder` bean without Spring Security filter chain or JPA.
- **`backend/src/main/java/com/example/courserec/auth/WebCorsConfig.java`**: Configured CORS for `/api/**` supporting frontend development on Vite (`localhost:5173`) and cross-origin clients.
- **`backend/src/main/java/com/example/courserec/auth/ApiException.java` & `GlobalExceptionHandler.java`**: Standardized error handling returning HTTP 4xx/5xx with `{ "error": "human-readable message" }`.
- **`backend/src/main/java/com/example/courserec/auth/AuthenticatedUser.java`**: Public record `(long id, String email, String name, String role)` adhering strictly to the contract.
- **`backend/src/main/java/com/example/courserec/auth/AuthService.java`**:
  - `requireUser(String authorizationHeader)`: Validates bearer token against SHA-256 session hash and unexpired timestamp; returns `AuthenticatedUser`.
  - `requireAdmin(String authorizationHeader)`: Enforces `ADMIN` role check.
  - Random 256-bit cryptographically secure bearer token generation (`createSession`).
  - Registration with validation (password >= 8 chars, email uniqueness).
  - Login with BCrypt verification.
- **`backend/src/main/java/com/example/courserec/auth/AuthController.java`**:
  - `POST /api/auth/register` -> `{token, user: {id, name, email, role}}`
  - `POST /api/auth/login` -> `{token, user: {id, name, email, role}}`
  - `GET /api/auth/me` -> `{id, name, email, role}`

### 4. User Profile Management
- **`backend/src/main/java/com/example/courserec/profile/ProfileDto.java`**: `SkillDto`, `ProfileResponse`, and `ProfileUpdateRequest`.
- **`backend/src/main/java/com/example/courserec/profile/ProfileService.java`**:
  - `getProfile(userId)`: Reads user details, interests list, and skills list.
  - `updateProfile(userId, request)`: Transactionally updates user attributes, replacing interests and skills.
  - `deleteAccount(userId)`: Deletes user record; foreign key cascade automatically cleans up interests, skills, sessions, and saved courses.
- **`backend/src/main/java/com/example/courserec/profile/ProfileController.java`**:
  - `GET /api/profile`
  - `PUT /api/profile`
  - `DELETE /api/profile`

### 5. Verification & Tests
- **`backend/src/test/java/com/example/courserec/auth/AuthAndProfileIntegrationTest.java`**: Full integration test covering demo course seeding, registration, duplicate checks, password length checks, login, token authentication, admin role guard, profile retrieval, profile updates, and account deletion.
- **Build & Test Runs**:
  - `mvn test`: 21 tests run, 0 failures, 0 errors across all modules.
  - `mvn -q -DskipTests package`: Built `courserec-backend-0.0.1-SNAPSHOT.jar` successfully.
