# --- Build stage ---
FROM gradle:8.8-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle clean build -x test --no-daemon

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as a non-root user
RUN groupadd --system procurement && useradd --system --gid procurement procurement
RUN mkdir -p /app/uploads && chown -R procurement:procurement /app

COPY --from=build /app/build/libs/*.jar /app/app.jar

USER procurement
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV UPLOAD_DIR=/app/uploads

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
