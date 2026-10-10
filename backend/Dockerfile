# Build the executable Spring Boot JAR with Java 21 and Maven.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Resolve dependencies first so Docker can reuse this layer between source edits.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

# Keep the runtime image smaller and run the application as an unprivileged user.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app

COPY --from=build --chown=10001:10001 \
    /workspace/target/freelance-agency-suite-1.0.0.jar \
    /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
USER 10001:10001

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
