#!/bin/bash

# =============================================================================
# Fast Deployment Script for NTSAL AI Knowledge Hub
# Strategy: Build locally with Maven, deploy with Docker Compose
# =============================================================================

set -e

echo "🚀 NTSAL AI Knowledge Hub - Fast Deployment"
echo "==========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
JAR_FILE="target/ntsal_ai_knowledge_hub-0.0.1-SNAPSHOT.jar"

# Check if JAR exists, build if needed
echo "🔍 Checking for compiled JAR..."
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}⚠️  JAR not found. Building with Maven...${NC}"
    echo ""
    mvn clean package -DskipTests -T 1C
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Maven build failed${NC}"
        exit 1
    fi
    echo ""
fi
echo -e "${GREEN}✅ JAR File: $JAR_FILE${NC}"
ls -lh "$JAR_FILE"
echo ""

# Verify Docker
echo "🔍 Checking Docker installation..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker found${NC}"

# Detect docker-compose command (V1 or V2)
echo "🔍 Checking Docker Compose..."
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version &> /dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
else
    echo -e "${RED}❌ Docker Compose not found${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Using: $DOCKER_COMPOSE${NC}"
echo ""

# Setup .env if needed
if [ ! -f .env ]; then
    echo "⚙️  Creating .env file..."
    cp .env.example .env
    echo -e "${YELLOW}⚠️  Please edit .env with your actual values:${NC}"
    echo "     GITHUB_TOKEN=your_token"
    echo "     OPENAI_API_KEY=your_key"
    echo "     POSTGRES_PASSWORD=secure_password"
    echo ""
fi

# Stop any existing containers first
echo "🔄 Stopping any existing containers..."
$DOCKER_COMPOSE down -v --remove-orphans 2>/dev/null || true

# Stop and remove old containers that might conflict
echo "🧹 Cleaning up old containers..."
docker ps -a | grep -E "ntsal_postgres|ntsal_app|ntsal_embedding|ntsal_mcp" | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true

# Remove old networks
docker network ls | grep ntsal | awk '{print $1}' | xargs -r docker network rm 2>/dev/null || true

# Check for port conflicts and kill processes if needed
echo "🔍 Checking for port conflicts..."
PORTS_TO_CHECK="5434 8080 8081 8000 3000"
RETRY_COUNT=0
MAX_RETRIES=3

retry_port_cleanup() {
    for PORT in $PORTS_TO_CHECK; do
        # Check for non-Docker processes using the port
        if lsof -ti:$PORT >/dev/null 2>&1; then
            PID=$(lsof -ti:$PORT 2>/dev/null | head -1)
            if [ ! -z "$PID" ]; then
                echo -e "${YELLOW}⚠️  Killing process $PID using port $PORT${NC}"
                kill -9 $PID 2>/dev/null || true

                # Wait for port to be released
                echo -n "   Waiting for port $PORT to be released..."
                for i in {1..20}; do
                    if ! lsof -ti:$PORT >/dev/null 2>&1; then
                        echo " Done"
                        break
                    fi
                    sleep 0.5
                    echo -n "."
                done
                echo ""
            fi
        fi
    done
}

retry_port_cleanup

# Double-check port 3000 specifically
echo "Verifying port 3000 is free..."
for attempt in {1..5}; do
    if ! lsof -ti:3000 >/dev/null 2>&1; then
        echo "✓ Port 3000 is free"
        break
    fi
    echo "  Attempt $attempt: Port 3000 still in use, waiting..."
    sleep 1
    lsof -ti:3000 2>/dev/null | xargs -r kill -9 || true
done

# Final wait for all ports to be released
sleep 2

echo ""

# Deploy
echo "🐳 Starting deployment..."
echo ""


# Start all services
echo "Starting containers (this may take a moment)..."
$DOCKER_COMPOSE up -d

# Check if MCP server failed to start due to port conflict
# If so, retry after a delay
echo "Checking for MCP server port conflicts..."
sleep 5
if ! docker ps --filter "name=ntsal_mcp_server" --filter "status=running" | grep -q ntsal_mcp_server; then
    echo -e "${YELLOW}⚠️  MCP Server failed to start, likely due to port 3000 being in use${NC}"
    echo "Killing process on port 3000..."
    lsof -ti:3000 2>/dev/null | xargs -r kill -9 || true
    sleep 3
    echo "Restarting MCP Server..."
    $DOCKER_COMPOSE up -d mcp_server 2>&1 | grep -v "already in use" || true
    sleep 5

    # Check again
    if ! docker ps --filter "name=ntsal_mcp_server" --filter "status=running" | grep -q ntsal_mcp_server; then
        echo -e "${RED}⚠️  MCP Server still failed to start${NC}"
        echo "   The MCP server is optional. Continuing with deployment..."
        echo "   You can manually restart it later with: docker compose up -d mcp_server"
    fi
fi

echo ""
echo "⏳ Waiting for services (up to 60 seconds)..."
echo ""

# Wait for PostgreSQL
printf "%-25s " "PostgreSQL:"
max_attempts=60
attempt=0
while ! $DOCKER_COMPOSE exec -T postgres pg_isready -U postgres >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ $attempt -gt $max_attempts ]; then
        echo -e "${RED}TIMEOUT${NC}"
        exit 1
    fi
    printf "."
    sleep 1
done
echo -e "${GREEN}READY${NC}"

# Wait for Embedding Service
printf "%-25s " "Embedding Service:"
attempt=0
while ! curl -s http://localhost:8000/health >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ $attempt -gt $max_attempts ]; then
        echo -e "${RED}TIMEOUT${NC}"
        exit 1
    fi
    printf "."
    sleep 1
done
echo -e "${GREEN}READY${NC}"

# Wait for MCP Server
printf "%-25s " "MCP Server:"
attempt=0
while ! curl -s http://localhost:3000/health >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ $attempt -gt $max_attempts ]; then
        echo -e "${YELLOW}TIMEOUT (optional)${NC}"
        break
    fi
    printf "."
    sleep 1
done
if [ $attempt -le $max_attempts ]; then
    echo -e "${GREEN}READY${NC}"
fi

# Wait for Spring Boot
printf "%-25s " "Spring Boot API:"
attempt=0
APP_PORT=$(grep "^APP_PORT=" .env 2>/dev/null | cut -d'=' -f2)
APP_PORT=${APP_PORT:-8080}
while ! curl -s http://localhost:$APP_PORT/api/ai/health >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ $attempt -gt $max_attempts ]; then
        echo -e "${RED}TIMEOUT${NC}"
        echo ""
        echo "Recent logs:"
        $DOCKER_COMPOSE logs app | tail -10
        exit 1
    fi
    printf "."
    sleep 1
done
echo -e "${GREEN}READY${NC}"

# Verify API
echo ""
echo "✅ Verifying API..."
response=$(curl -s http://localhost:$APP_PORT/api/ai/health)
if echo "$response" | grep -q "healthy"; then
    echo -e "${GREEN}✅ API Health: OK${NC}"
else
    echo -e "${RED}❌ API Health: FAILED${NC}"
    echo "Response: $response"
    exit 1
fi

# Print success message
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo -e "${GREEN}🎉  DEPLOYMENT SUCCESSFUL!${NC}"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Get actual PostgreSQL port from .env
POSTGRES_PORT=$(grep "^POSTGRES_PORT=" .env 2>/dev/null | cut -d'=' -f2)
POSTGRES_PORT=${POSTGRES_PORT:-5432}

echo "📍 Service URLs:"
echo "   API:       http://localhost:$APP_PORT"
echo "   Embedding: http://localhost:8000"
echo "   Database:  localhost:$POSTGRES_PORT"
echo ""
echo "📚 Quick Commands:"
echo "   Health check:   ./health_check.sh"
echo "   View logs:      $DOCKER_COMPOSE logs -f app"
echo "   Stop services:  $DOCKER_COMPOSE down"
echo ""
echo "📝 Configuration:"
echo "   Edit .env with your GitHub token and API keys"
echo ""
echo "🚀 Next Steps:"
echo "   1. Make sure .env has your actual credentials"
echo "   2. Add a GitHub repo:  curl -X POST http://localhost:$APP_PORT/api/github-repos ..."
echo "   3. Collect commits:    curl -X POST http://localhost:$APP_PORT/api/commits/collect"
echo "   4. Query with AI:      curl -X POST http://localhost:$APP_PORT/api/ai/query ..."
echo ""

