# Shared implementation contract

## Backend conventions

- Java package root: `com.example.courserec`; Maven project in `backend/`.
- Spring Boot 3.x, Java 21 target. Use Spring Web, Spring JDBC, validation where useful, MySQL driver, H2, and Spring Security Crypto for password hashing only. No Spring Security filter chain or JPA.
- JSON API prefix `/api`. API errors: HTTP 4xx/5xx with `{ "error": "human-readable message" }`. All request bodies are JSON. Use CORS for local Vite or its `/api` proxy.
- Auth header: `Authorization: Bearer <token>`. `AuthService.requireUser(String authorizationHeader)` returns an `AuthenticatedUser` record with `long id`, `String email`, `String name`, `String role` and throws a status-bearing exception on missing/invalid token. `AuthService.requireAdmin(String authorizationHeader)` additionally requires role `ADMIN`. Foundation session owns these types in `auth/`.
- Course/rec sessions may use `JdbcTemplate` directly and must use parameter binding for user input.
- API responses and DTO fields use camelCase. IDs are numeric. Dates are ISO 8601 strings.

## SQL schema (foundation session owns `backend/src/main/resources/schema.sql`)

- `users(id BIGINT PK AUTO_INCREMENT, name VARCHAR(120), email VARCHAR(255) UNIQUE, password_hash VARCHAR(255), role VARCHAR(20), academic_background VARCHAR(255), proficiency_level VARCHAR(30), career_goal VARCHAR(255))`
- `user_interests(id BIGINT PK AUTO_INCREMENT, user_id BIGINT FK, interest_name VARCHAR(100))`
- `user_skills(id BIGINT PK AUTO_INCREMENT, user_id BIGINT FK, skill_name VARCHAR(100), proficiency VARCHAR(30))`
- `courses(id BIGINT PK AUTO_INCREMENT, title VARCHAR(255), description TEXT, category VARCHAR(100), difficulty_level VARCHAR(30), duration_hours INT, platform VARCHAR(120), course_url VARCHAR(500), career_goals VARCHAR(500), prerequisites VARCHAR(500), is_demo BOOLEAN)`
- `course_topics(id BIGINT PK AUTO_INCREMENT, course_id BIGINT FK, topic_name VARCHAR(100))`
- `course_skills(id BIGINT PK AUTO_INCREMENT, course_id BIGINT FK, skill_name VARCHAR(100))`
- `saved_courses(id BIGINT PK AUTO_INCREMENT, user_id BIGINT FK, course_id BIGINT FK, saved_at TIMESTAMP, UNIQUE(user_id,course_id))`
- `sessions(id BIGINT PK AUTO_INCREMENT, user_id BIGINT FK, token_hash VARCHAR(64) UNIQUE, expires_at TIMESTAMP)`
- Foreign keys cascade delete where appropriate. Seed at least 12 varied demo courses plus topics and skills, idempotently.

## Auth and profile API (session 1)

- `POST /api/auth/register` `{name,email,password}` -> `{token,user:{id,name,email,role}}`. Password >= 8 characters; email unique.
- `POST /api/auth/login` `{email,password}` -> same.
- `GET /api/auth/me` -> `{id,name,email,role}`.
- `GET /api/profile` -> `{id,name,email,academicBackground,proficiencyLevel,careerGoal,interests:[string],skills:[{name,proficiency}]}`.
- `PUT /api/profile` accepts `{name,academicBackground,proficiencyLevel,careerGoal,interests:[string],skills:[{name,proficiency}]}` -> updated profile.
- `DELETE /api/profile` deletes the authenticated user's account.
- Optional admin bootstrap: if `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set, create/admin-upgrade that account at startup. Never hardcode a default admin password.

## Catalog and saved API (session 3)

- Public `GET /api/courses?q=&topic=&level=&platform=&maxDurationHours=` -> array of courses.
- Public `GET /api/courses/{id}` -> course object.
- Course object `{id,title,description,category,difficultyLevel,durationHours,platform,courseUrl,careerGoals:[string],prerequisites:[string],topics:[string],skills:[string],demo:boolean}`.
- Admin `POST /api/admin/courses`, `PUT /api/admin/courses/{id}`, `DELETE /api/admin/courses/{id}`; course input is the course object without `id`/`demo`, plus optional `demo`.
- Student `GET /api/saved` -> array of course objects; `POST /api/saved/{courseId}` -> 204, idempotent; `DELETE /api/saved/{courseId}` -> 204.

## Recommendation API (session 2)

- Authenticated `GET /api/recommendations` -> array of `{course:<course object>,score:number,components:{interest:number,career:number,skill:number,level:number},reasons:[string]}` ordered by descending score, then ascending course ID.
- Reuse the course JSON shape above. It may query courses directly, or use a catalog service only if one exists; avoid a compile-time dependency on session 3 code.
- Normalize tokens case-insensitively, trim whitespace, score missing profile fields without division by zero, and explain notable matches or prerequisites. Each component is 0–100.

## Frontend (session 4)

- React + Vite in `frontend/`; use plain CSS and accessible labels. Vite proxy `/api` to `http://localhost:8080`.
- Views: welcome/login/register, profile editor, recommendations with score/explanations, catalog search/filters, saved courses, admin CRUD visible only to admins.
- Persist bearer token in local storage for this academic prototype. On 401 clear session and show login. Show loading, empty, validation, and API-error states.
- No mocked API responses in the delivered app.
