# Multi-stage Docker build for TIET Student Portal
# Stage 1: Build standalone executable fat JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Lightweight runtime environment
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/app.jar ./app.jar
COPY --from=build /app/src/main/webapp ./src/main/webapp

# Environment variable PORT can be overridden dynamically by cloud providers (Render, Railway, etc.)
ENV PORT=8080
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
