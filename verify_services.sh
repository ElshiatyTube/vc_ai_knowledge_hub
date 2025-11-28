#!/bin/bash

# =============================================================================
# NTSAL AI Knowledge Hub - Service Verification Script (AI Generated)
# =============================================================================

echo "🔍 NTSAL AI Knowledge Hub - Service Status Check"
echo "================================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check Docker containers
echo "📦 Checking containers..."
echo ""

services=("ntsal_app" "ntsal_postgres" "ntsal_embedding_service" "ntsal_mcp_server")
all_healthy=true

for service in "${services[@]}"; do
    STATUS=$(docker ps --filter "name=$service" --format "{{.Status}}" 2>/dev/null)

    if [ -z "$STATUS" ]; then
        echo -e "${RED}❌ $service: Not running${NC}"
        all_healthy=false
    elif echo "$STATUS" | grep -q "(healthy)"; then
        echo -e "${GREEN}✅ $service: Healthy${NC}"
    elif echo "$STATUS" | grep -q "Up"; then
        echo -e "${YELLOW}⚠️  $service: Running (checking health)${NC}"
    else
        echo -e "${RED}❌ $service: $STATUS${NC}"
        all_healthy=false
    fi
done

echo ""
echo "🌐 Checking health endpoints..."
echo ""

# Test API
echo -n "API Health:       "
if curl -s http://localhost:8081/api/ai/health 2>/dev/null | grep -q "healthy"; then
    echo -e "${GREEN}✅ OK${NC}"
else
    echo -e "${RED}❌ FAILED${NC}"
    all_healthy=false
fi

# Test Embedding
echo -n "Embedding Health: "
if curl -s http://localhost:8000/health 2>/dev/null | grep -q "healthy"; then
    echo -e "${GREEN}✅ OK${NC}"
else
    echo -e "${RED}❌ FAILED${NC}"
fi

# Test Database
echo -n "Database Health:  "
if docker exec ntsal_postgres pg_isready -U postgres >/dev/null 2>&1; then
    echo -e "${GREEN}✅ OK${NC}"
else
    echo -e "${RED}❌ FAILED${NC}"
    all_healthy=false
fi

# Test MCP
echo -n "MCP Server Health: "
if curl -s http://localhost:3000/health >/dev/null 2>&1; then
    echo -e "${GREEN}✅ OK${NC}"
else
    echo -e "${YELLOW}⚠️  Optional (not critical)${NC}"
fi

echo ""
echo "📍 Service URLs:"
echo "   API:       http://localhost:8081"
echo "   Embedding: http://localhost:8000"
echo "   Database:  localhost:5434"
echo "   MCP:       http://localhost:3000"
echo ""

if [ "$all_healthy" = true ]; then
    echo -e "${GREEN}✅ All critical services are healthy!${NC}"
    echo ""
    echo "🚀 You can now:"
    echo "   1. Add repositories: curl -X POST http://localhost:8081/api/github-repos ..."
    echo "   2. Collect commits:  curl -X POST http://localhost:8081/api/commits/collect"
    echo "   3. Query with AI:    curl -X POST http://localhost:8081/api/ai/query ..."
    exit 0
else
    echo -e "${RED}❌ Some services are not healthy${NC}"
    echo ""
    echo "📋 Troubleshooting:"
    echo "   1. Check logs:  docker compose logs -f"
    echo "   2. Restart:     docker compose restart"
    echo "   3. Restart MCP: docker compose up -d mcp_server"
    exit 1
fi

