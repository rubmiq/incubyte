# syntax=docker/dockerfile:1

# --- Stage 1: build the Angular production bundle ---
FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# --- Stage 2: build the Spring Boot jar, embedding the Angular bundle as static resources ---
FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /backend
COPY backend/.mvn/ .mvn/
COPY backend/mvnw ./
COPY backend/pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY backend/src/ src/
COPY --from=frontend-build /frontend/dist/salary-management-ui/browser/ src/main/resources/static/
RUN ./mvnw -q -B package -DskipTests

# --- Stage 3: minimal runtime image ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend-build /backend/target/salary-service-*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
