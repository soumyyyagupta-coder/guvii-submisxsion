# CourseRec

React web app and Java REST API for personalized course recommendations. The attached project report informed the feature set. The interface is web based as requested. Ranking uses transparent weighted content matching; it is not a trained machine-learning model.

## Run locally

Requirements: Java 21 or newer, Maven 3.9+, Node.js, npm.

In one terminal:

```powershell
cd backend
mvn spring-boot:run
```

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the URL printed by Vite (normally `http://localhost:5173`). The API runs on `http://localhost:8080`; Vite proxies `/api` requests to it. The default H2 SQL database is stored under `backend/data/` and seeds 14 clearly labeled demo courses on first launch. Demo course links and availability are illustrative and have not been verified against providers.

To enable an administrator account, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` in the backend terminal before launch, then log in with those credentials. No administrator password is included in the repository.

To use MySQL, create an empty database and set `SPRING_PROFILES_ACTIVE=mysql`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in the backend terminal. Example URL: `jdbc:mysql://localhost:3306/courserec`. The app creates its SQL tables on startup. The default H2 database is sufficient for local use.

## Verify

```powershell
cd backend
mvn test
cd ..\frontend
npm run build
```

With the API running, `python scripts/smoke.py` exercises registration, profile editing, catalog search, recommendations, saves, and student access control using a disposable account.
Set `COURSE_REC_API_BASE=http://localhost:10000/api` to run the same check against the Docker container.

## One-container Docker deployment

The root [Dockerfile](Dockerfile) builds React, embeds its production files in the Spring Boot jar, and installs PostgreSQL in the same container. The startup script initializes PostgreSQL, creates the database, then starts the Java/JDBC API on Render's `PORT`. PostgreSQL listens only on container loopback; users access the React app and API through the single web port. The jar includes the PostgreSQL JDBC driver, a PostgreSQL schema, and code that seeds 14 demo courses. It does **not** copy `backend/data/` into the image; that local H2 database contains development accounts and is excluded by `.dockerignore`.

```powershell
docker build -t courserec-render .
docker run --rm -p 10000:10000 courserec-render
```

Open `http://localhost:10000` and check `http://localhost:10000/api/health`. The React app and API share one origin, so the Vite development proxy is not used in the container.

### Render Free

The root [render.yaml](render.yaml) defines a single Docker web service on Render's Free plan. Put this project in a Git repository on GitHub or another Git provider supported by Render, then create a Render Blueprint from that repository. Render builds the Dockerfile and serves the app at its assigned `onrender.com` URL. The Blueprint health check is `/api/health`. Add `ADMIN_EMAIL` and `ADMIN_PASSWORD` in the Render dashboard if you need an admin login; keep these values out of source control.

**Free-tier data limit:** PostgreSQL runs inside the web service, so its data files are on Render Free's temporary filesystem. Render Free web services lose filesystem changes whenever they spin down, restart, or redeploy. The 14 demo courses are recreated automatically, but registered users, profile changes, and bookmarks are lost. Render Free web services cannot attach a persistent disk. Long-lived user data needs a separate persistent database or a paid disk. Render's separate Free Postgres option currently expires after 30 days. See [Render's Free plan documentation](https://render.com/docs/free).

The existing MySQL JDBC profile can connect to an external MySQL database by setting `SPRING_PROFILES_ACTIVE=mysql` and the JDBC environment variables listed above; this is separate from the one-container demo setup.

## Design and task history

See [PLAN.md](PLAN.md) for the implementation plan, [API_CONTRACT.md](API_CONTRACT.md) for endpoint and schema contracts, and [TASKS](TASKS) for the four disjoint `agy` assignments and result notes. See [REVIEW.md](REVIEW.md) for integration findings.
