# ATMConnect Dockerfile
# Multi-stage build for optimal production image size and security

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Install build dependencies
RUN apk add --no-cache \
    curl \
    git \
    bash

# Copy Maven wrapper and configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build application
RUN ./mvnw clean package -DskipTests -B

# Verify JAR was created
RUN ls -la target/ && \
    test -f target/*.jar

# Runtime stage - Development
FROM eclipse-temurin:21-jre-alpine AS development

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    bash \
    tzdata \
    dumb-init

# Create application user
RUN addgroup -g 1001 atmconnect && \
    adduser -D -s /bin/bash -u 1001 -G atmconnect atmconnect

# Set working directory
WORKDIR /app

# Create directories
RUN mkdir -p /app/logs /app/config /app/certs && \
    chown -R atmconnect:atmconnect /app

# Copy JAR from builder
COPY --from=builder --chown=atmconnect:atmconnect /app/target/*.jar app.jar

# Copy configuration files
COPY --chown=atmconnect:atmconnect src/main/resources/application.yml /app/config/

# Create health check script
RUN echo '#!/bin/bash\ncurl -f http://localhost:8080/api/v1/health || exit 1' > /app/healthcheck.sh && \
    chmod +x /app/healthcheck.sh && \
    chown atmconnect:atmconnect /app/healthcheck.sh

# Switch to non-root user
USER atmconnect

# Expose ports
EXPOSE 8080 8443

# Environment variables
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=development \
    TZ=UTC

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD ["/app/healthcheck.sh"]

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Runtime stage - Production
FROM eclipse-temurin:21-jre-alpine AS production

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    bash \
    tzdata \
    dumb-init \
    ca-certificates

# Update CA certificates
RUN update-ca-certificates

# Create application user
RUN addgroup -g 1001 atmconnect && \
    adduser -D -s /bin/bash -u 1001 -G atmconnect atmconnect

# Set working directory
WORKDIR /app

# Create directories with proper permissions
RUN mkdir -p /app/logs /app/config /app/certs /app/tmp && \
    chown -R atmconnect:atmconnect /app

# Copy JAR from builder
COPY --from=builder --chown=atmconnect:atmconnect /app/target/*.jar app.jar

# Copy configuration files
COPY --chown=atmconnect:atmconnect src/main/resources/application.yml /app/config/

# Create health check script
RUN echo '#!/bin/bash\ncurl -f http://localhost:8080/api/v1/health || exit 1' > /app/healthcheck.sh && \
    chmod +x /app/healthcheck.sh && \
    chown atmconnect:atmconnect /app/healthcheck.sh

# Remove unnecessary packages (security hardening)
RUN apk del --purge curl && \
    rm -rf /var/cache/apk/* /tmp/* /var/tmp/*

# Switch to non-root user
USER atmconnect

# Expose ports
EXPOSE 8080 8443

# Environment variables for production
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication -XX:+OptimizeStringConcat" \
    SPRING_PROFILES_ACTIVE=production \
    TZ=UTC \
    JAVA_SECURITY_EGD=file:/dev/./urandom

# Health check (using wget since curl was removed)
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD ["sh", "-c", "wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1"]

# Security labels
LABEL maintainer="ATMConnect Team <team@atmconnect.bank>" \
      version="1.0.0" \
      description="ATMConnect - Secure Banking Application with BLE Connectivity" \
      security.policy="restricted" \
      security.scan="required"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start application with production settings
CMD ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]