# Use openjdk:21-slim as a parent image
FROM openjdk:21-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file into the container (adjust the path if needed)
COPY build/libs/transaction-management-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 (if your Spring Boot app runs on port 8080)
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]