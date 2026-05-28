FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY backend/pom.xml ./pom.xml

COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache python3

WORKDIR /app

RUN addgroup -S hackerprank \
    && adduser -S hackerprank -G hackerprank \
    && mkdir -p /app/.hackerprank-submissions \
    && chown -R hackerprank:hackerprank /app

ENV HACKERPRANK_RUNNER_MODE=local \
    HACKERPRANK_RUNNER_WORKSPACE_ROOT=/app/.hackerprank-submissions

COPY --from=build /workspace/target/hackerprank-backend-0.1.0-SNAPSHOT.jar /app/app.jar

USER hackerprank

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
