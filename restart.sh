#!/bin/bash

# =============================================================================
# Simple Restart Script - Stops everything and starts fresh (AI Generated)
# =============================================================================

set -e

echo "🔄 Restarting NTSAL AI Knowledge Hub..."
echo ""

# Stop everything
echo "⏹️  Stopping all containers..."
docker compose down -v 2>/dev/null || true

# Kill any processes on our ports
echo "🔧 Freeing up ports..."
for PORT in 3000 8080 8081 5434 8000; do
    PID=$(lsof -ti:$PORT 2>/dev/null || true)
    if [ ! -z "$PID" ]; then
        echo "   Killing process $PID on port $PORT"
        kill -9 $PID 2>/dev/null || true
    fi
done

# Wait for ports to be released
sleep 3

# Clean up Docker
echo "🧹 Cleaning Docker..."
docker system prune -f > /dev/null 2>&1

# Start everything
echo "🚀 Starting all services..."
docker compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 15

# Check status
echo ""
echo "📊 Service Status:"
docker compose ps

echo ""
echo "✅ Restart complete!"
echo ""
echo "Service URLs:"
echo "  - API:       http://localhost:8081"
echo "  - Embedding: http://localhost:8000"
echo "  - Database:  localhost:5434"
echo ""
echo "Check logs with: docker compose logs -f app"

