FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B --no-transfer-progress clean test package

FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="Artifact Alley" \
      org.opencontainers.image.description="Spring Boot auction platform" \
      org.opencontainers.image.source="https://github.com/sahilyadav-15/artifact_alley"
WORKDIR /app
RUN groupadd --system --gid 10001 appuser \
    && useradd --system --uid 10001 --gid appuser --home-dir /app appuser \
    && mkdir -p /app/tmp /app/uploads/artifacts \
    && chown -R appuser:appuser /app/tmp /app/uploads
COPY --chown=root:root --from=build /workspace/target/artifact-alley-0.0.1-SNAPSHOT.war /app/app.war
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/app/tmp"
USER appuser
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "/app/app.war"]
