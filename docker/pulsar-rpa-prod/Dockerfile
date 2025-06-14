# Stage 1: Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /build

# Copy project files (use .dockerignore to control which files to copy)
COPY pom.xml ./
COPY VERSION ./
COPY .mvn ./.mvn
COPY bin ./bin
COPY . .

RUN ls -la

# Build the application with Maven cache mount
RUN --mount=type=cache,target=/root/.m2 \
    if [ -f "./bin/build.sh" ]; then \
        chmod +x ./bin/build.sh && ./bin/build.sh; \
    else \
        mvn clean package -DskipTests -Dmaven.javadoc.skip=true; \
    fi

# Copy JAR for use in the next stage with better error handling
RUN JAR_FILE=$(find . -name "PulsarRPA*.jar" -type f | head -n 1) && \
    test -n "$JAR_FILE" || (echo "ERROR: PulsarRPA JAR file not found" && exit 1) && \
    cp "$JAR_FILE" /build/app.jar && \
    echo "Successfully copied JAR: $JAR_FILE"

# Stage 2: Run stage
FROM eclipse-temurin:21-jre-alpine AS runner

# Set working directory
WORKDIR /app

# Set timezone
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Install Chromium and necessary dependencies with security updates
RUN apk update && apk upgrade && \
    apk add --no-cache \
    curl \
    chromium \
    nss \
    freetype \
    freetype-dev \
    harfbuzz \
    ca-certificates \
    ttf-freefont \
    dbus && \
    rm -rf /var/cache/apk/*

# Build arguments for secrets (can be passed at build time)
ARG DEEPSEEK_API_KEY
ARG PROXY_ROTATION_URL
ARG USE_HOST_MAVEN_REPO=false

# Set environment variables
# Configurable browser context mode with sensible defaults
ENV JAVA_OPTS="-Xms2G -Xmx10G -XX:+UseG1GC" \
    DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} \
    PROXY_ROTATION_URL=${PROXY_ROTATION_URL} \
    BROWSER_CONTEXT_MODE=${BROWSER_CONTEXT_MODE:-SEQUENTIAL} \
    BROWSER_CONTEXT_NUMBER=${BROWSER_CONTEXT_NUMBER:-2} \
    BROWSER_MAX_OPEN_TABS=${BROWSER_MAX_OPEN_TABS:-8} \
    BROWSER_DISPLAY_MODE=${BROWSER_DISPLAY_MODE:-HEADLESS} \
    SERVER_PORT=${SERVER_PORT:-8182} \
    SERVER_ADDRESS=${SERVER_ADDRESS:-0.0.0.0}

# Copy build artifact
COPY --from=builder /build/app.jar app.jar

# Expose port (documentation only)
# H2database, TCP
EXPOSE 8082
# PulsarRPA REST API Server, HTTP
EXPOSE 8182

# Create non-root user and set directory permissions
RUN addgroup --system --gid 1001 appuser && \
    adduser --system --uid 1001 --ingroup appuser appuser && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Add comprehensive labels
LABEL maintainer="Vincent Zhang <ivincent.zhang@gmail.com>" \
      description="PulsarRPA: An AI-Enabled, Super-Fast, Thread-Safe Browser Automation Solution! 💖" \
      org.opencontainers.image.source="https://github.com/platonai/PulsarRPAPro" \
      org.opencontainers.image.documentation="https://github.com/platonai/PulsarRPAPro/blob/master/README.md" \
      version="exotic-standalone"

# Startup command with configurable arguments
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS \
    -Dserver.port=${SERVER_PORT:-8182} \
    -Dserver.address=${SERVER_ADDRESS:-0.0.0.0} \
    -jar app.jar ${COMMAND_ARGS:-serve}"]
