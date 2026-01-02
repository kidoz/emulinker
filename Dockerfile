# Build stage
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

# Copy gradle files for caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Ensure gradlew is executable
RUN chmod +x gradlew

# Download dependencies (cache layer)
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src src
RUN ./gradlew bootJar --no-daemon

# Run stage
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# Copy the executable jar from the build stage
COPY --from=build /app/build/kaillux.jar .

# Copy configuration (expected to be present in the project root)
# If custom config files aren't in src/main/resources, they need to be provided
# For now, we'll assume the jar contains the defaults from src/main/resources

# Admin UI
EXPOSE 8080/tcp
# Kaillera Connect
EXPOSE 27888/udp
# Kaillera Data Ports (default range)
EXPOSE 27889-27924/udp

ENTRYPOINT ["java", "-jar", "kaillux.jar"]
