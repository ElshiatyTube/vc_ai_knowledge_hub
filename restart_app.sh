#!/bin/bash

# Quick restart script for Spring Boot application (AI Generated)

echo "🔄 Restarting Spring Boot Application"
echo "======================================"

# Find and kill existing Spring Boot process
echo "1️⃣  Stopping existing application..."
if [ -f logs/spring-boot.pid ]; then
    PID=$(cat logs/spring-boot.pid)
    if ps -p $PID > /dev/null 2>&1; then
        kill $PID
        echo "   ✓ Stopped process $PID"
        sleep 2
    fi
    rm logs/spring-boot.pid
else
    # Try to find by port
    PID=$(lsof -ti:8080 2>/dev/null)
    if [ -n "$PID" ]; then
        kill $PID
        echo "   ✓ Stopped process on port 8080"
        sleep 2
    fi
fi

echo ""
echo "2️⃣  Starting application..."
cd "$(dirname "$0")"

# Start in background
nohup mvn spring-boot:run > logs/spring-boot.log 2>&1 &
NEW_PID=$!
echo $NEW_PID > logs/spring-boot.pid

echo "   ✓ Started with PID: $NEW_PID"
echo ""
echo "3️⃣  Waiting for application to start..."
sleep 5

# Check if running
if ps -p $NEW_PID > /dev/null; then
    echo "   ✓ Application is running"
    echo ""
    echo "View logs:"
    echo "  tail -f logs/spring-boot.log"
    echo ""
    echo "Test health:"
    echo "  curl http://localhost:8080/api/ai/health"
else
    echo "   ✗ Application failed to start"
    echo "   Check logs: cat logs/spring-boot.log"
    exit 1
fi

echo ""
echo "======================================"
echo "✅ Application restarted successfully"
echo "======================================"

