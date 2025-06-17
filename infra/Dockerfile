# ---------- Stage 1: Build the JAR ----------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy only the pom.xml and download dependencies (for layer caching)
COPY ../pom.xml .
RUN mvn dependency:go-offline

# Copy the source code
COPY ../src ./src

# Package the application
RUN mvn clean package -DskipTests

# ---------- Stage 2: Run the JAR ----------
FROM openjdk:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/instagram-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]
