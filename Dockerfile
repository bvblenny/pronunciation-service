# syntax=docker/dockerfile:1

# ===== Build stage =====
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY . .
# Speed up: use Gradle cache layers
RUN gradle --no-daemon clean bootJar -x test

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
ENV TZ=UTC \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=default \
    FFMPEG_PATH=ffmpeg \
    PORT=8080

# Install ffmpeg (Debian-based image)
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:${PORT}/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --server.port=$PORT"]

