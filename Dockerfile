# Use an official OpenJDK image as the base
FROM openjdk:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the jar file into the container
COPY target/instagram-backend-0.0.1-SNAPSHOT.jar app.jar


# Expose port (optional but good practice)
EXPOSE 8080

# Run the jar file
CMD ["java", "-jar", "app.jar"]
