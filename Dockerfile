# Build stage
FROM maven:3.8.8-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy
COPY --from=build target/wastemanagement-0.0.1-SNAPSHOT.jar wastemanagement.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=${PORT:8080}", "-jar", "/wastemanagement.jar"]
