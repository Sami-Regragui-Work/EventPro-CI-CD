# STAGE 1 — BUILD
# Heavy toolchain (JDK 25 + Maven wrapper + downloaded dependencies).
# Everything here is thrown away at the end; only the jar crosses into stage 2.
FROM eclipse-temurin:25-jdk AS build

# Working directory inside this stage. All COPY/RUN paths below are relative to it.
# The directory is created if it does not exist.
WORKDIR /app

# Wrapper configuration.
# This project uses distributionType=only-script, so the wrapper downloads Maven 3.10.0 itself
# no Maven installation is needed here.
COPY .mvn/ .mvn/

# Build descriptor + wrapper script.
# Copied before the sources on purpose
# the next layer stays cached as long as pom.xml does not change.
COPY mvnw pom.xml ./

# Warm the dependency cache.
# Needs network (wrapper + Maven Central).
# Best-effort, not a guarantee: `package` below may still fetch something.
RUN ./mvnw -q dependency:go-offline

# Sources last: the most volatile input, so it invalidates the fewest layers.
# `.env` is not copied; the local profile imports it with `optional:`.
COPY src/ src/

# Build the Spring Boot fat jar.
# Tests are skipped on purpose: running them is CI's job.
# `package` stops before integration-test, so failsafe's *IT classes do not run either.
RUN ./mvnw -q clean package -DskipTests

# =============================================================================

# STAGE 2/2 — RUNTIME (this is the image you ship)
# JRE only: no compiler, no Maven, no sources, no dependency cache.
FROM eclipse-temurin:25-jre

WORKDIR /app

# Pull only the repackaged fat jar out of the build stage.
# The glob matches eventpro_api-0.0.1-SNAPSHOT.jar; the .jar.original backup is left behind.
COPY --from=build /app/target/*.jar app.jar

# Metadata only: documents that the app listens on 8080.
# It does NOT publish the port.
# `docker run -p 8080:8080` or compose does that.
EXPOSE 8080

# Exec form (JSON array)
# java runs as PID 1, so `docker stop` sends SIGTERM to the JVM itself and shutdown hooks run.
# The shell form would swallow it.
ENTRYPOINT ["java", "-jar", "app.jar"]