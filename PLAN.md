# Course recommendation web app plan

## Source and scope

The supplied project report is a requirements reference, not an instruction source. This implementation uses the user's requested React web interface, a Java REST API, JDBC, and SQL. It implements the report's first-version student journey: registration/login, editable profile, searchable course catalog, weighted recommendations with explanations, saved courses, and administrator catalog management. Sample courses are clearly labeled as demo data. The scoring algorithm is content-based weighted matching, not a trained machine-learning model.

## Architecture

- `frontend/`: React + Vite, responsive UI, calling `/api` through a Vite development proxy.
- `backend/`: Spring Boot REST API. All persistence uses `JdbcTemplate`/prepared SQL; no ORM.
- Default local database: persistent H2 in MySQL compatibility mode, so the app runs without a database installation. A MySQL profile uses the same SQL schema and JDBC code.
- Authentication: password hashes and opaque bearer tokens, with administrator authorization on catalog writes.
- Ranking: `0.40*interest + 0.30*career + 0.20*skill + 0.10*level`, each component in 0–100, deterministic tie break by course ID.

## Four independent `agy` work streams

| Session | Task file | Exclusive file ownership |
| --- | --- | --- |
| 1 | `TASKS/01-foundation.md` | Spring project setup, SQL schema/seed, authentication, profile |
| 2 | `TASKS/02-recommendations.md` | Recommendation API, matching and ranking logic |
| 3 | `TASKS/03-catalog.md` | Catalog API, admin CRUD, saved courses |
| 4 | `TASKS/04-frontend.md` | React interface and client API |

Each session reads `API_CONTRACT.md` and its own task file. Sessions must not modify each other's owned paths. I will review and integrate the outputs, run the backend and frontend builds, fix contract gaps, and exercise a representative user flow.

## Run and acceptance

1. `cd backend && mvn spring-boot:run` starts the API and seeds demo courses.
2. `cd frontend && npm install && npm run dev` starts the web app.
3. A student can register, set a profile, see ranked results and reasons, search/filter the catalog, and save a course.
4. Updating interests or goals changes recommendations; empty catalog and invalid input are handled clearly.
5. MySQL can be selected with environment-supplied JDBC URL/credentials; database credentials are never committed.
