# ---- Stage 1: build the Spring Boot jar with Maven on JDK 17 ----
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy the build descriptor first so Docker can cache the dependency layer.
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ---- Stage 2: run it on a slim JRE-only image ----
FROM eclipse-temurin:17.0.15_6-jre

WORKDIR /app

# Run as an unprivileged user instead of root.
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /app/target/back-end-0.0.1-SNAPSHOT.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser

# The Spring Boot server listens on 8080.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
