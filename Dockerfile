# =============================================================================
# Multi-stage Dockerfile for NTSAL AI Knowledge Hub
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build stage
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy Maven files first (for better layer caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies with retry logic (cached layer if pom.xml doesn't change)
RUN mvn dependency:go-offline -B -DoutputFile=/dev/null || \
    mvn dependency:go-offline -B -DoutputFile=/dev/null || \
    mvn dependency:go-offline -B -DoutputFile=/dev/null

# Copy source code
COPY src ./src

# Build the application with retry logic (skip tests for faster builds)
RUN mvn clean package -DskipTests -B -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=5 || \
    mvn clean package -DskipTests -B -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=5 || \
    mvn clean package -DskipTests -B -Dmaven.wagon.http.retryHandler.class=standard -Dmaven.wagon.http.retryHandler.count=5

# Verify the JAR was created
RUN ls -lh target/*.jar && echo "Build successful!"

# -----------------------------------------------------------------------------
# Stage 2: Runtime stage
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Copy the built JAR from build stage
COPY --from=build /build/target/*.jar app.jar

# Create directories for logs and temp
RUN mkdir -p /app/logs /app/temp && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/ai/health || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

