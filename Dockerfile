# Build the React client and serve it from the same Spring Boot process as the API.
FROM node:20-alpine AS frontend-build
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline
COPY backend/src/ ./src/
COPY --from=frontend-build /build/frontend/dist/ ./src/main/resources/static/
RUN mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends postgresql-16 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=backend-build /build/backend/target/courserec-backend-0.0.1-SNAPSHOT.jar ./app.jar
COPY docker/start.sh ./start.sh
RUN chmod 755 /app/start.sh \
    && mkdir -p /app/pgdata \
    && chown postgres:postgres /app/pgdata

ENV PORT=10000 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=45.0 -XX:+UseSerialGC" \
    SPRING_PROFILES_ACTIVE=postgres \
    SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/courserec \
    SPRING_DATASOURCE_USERNAME=postgres \
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3
EXPOSE 10000
CMD ["/app/start.sh"]
