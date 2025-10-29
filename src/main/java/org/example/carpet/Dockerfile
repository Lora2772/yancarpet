# =========
# Stage 1: Build the application with Maven
# =========
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first and resolve dependencies (layer caching)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copy source code
COPY src ./src

# Build the Spring Boot fat jar
RUN mvn -q clean package -DskipTests

# =========
# Stage 2: Run the application
# =========
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the same port as in application.yml
EXPOSE 8080

# How to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
