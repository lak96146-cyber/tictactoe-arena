# ===============================================================
#   TIC-TAC-TOE ARENA — DOCKERFILE FOR RENDER WEB SERVICE
#   Multi-stage build targeting OpenJDK 21 (Alpine Linux)
# ===============================================================

# --- Stage 1: Build & Package ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy source tree and persistent initial data
COPY src ./src
COPY data ./data

# Compile all Java modules
RUN mkdir -p bin && \
    javac -encoding UTF-8 -d bin $(find src -name "*.java")

# Package standalone server JAR (headless, zero GUI dependencies)
RUN jar --create --file tictactoe-server.jar \
        --main-class server.GameServer \
        -C bin server \
        -C bin protocol \
        -C bin game \
        -C bin history

# --- Stage 2: Minimal Production JRE Runtime ---
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Install curl for container health check support
RUN apk add --no-cache curl

# Copy compiled server JAR and data store from builder
COPY --from=builder /build/tictactoe-server.jar /app/tictactoe-server.jar
COPY --from=builder /build/data /app/data

# Create dedicated non-root user for production security
RUN addgroup -S arenagroup && adduser -S arenauser -G arenagroup && \
    chown -R arenauser:arenagroup /app

USER arenauser

# Default port for Render (Render injects PORT dynamically at runtime)
ENV PORT=10000
ENV ARENA_STORAGE=csv
ENV JAVA_OPTS="-Djava.awt.headless=true -Xms64m -Xmx256m"

EXPOSE 10000

# Docker healthcheck against the built-in HTTP endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:${PORT}/health || exit 1

# Launch the server listening on 0.0.0.0:$PORT
CMD ["sh", "-c", "java $JAVA_OPTS -jar tictactoe-server.jar ${PORT}"]
