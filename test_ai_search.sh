#!/bin/bash

# AI Search Testing Script (AI Generated)
# Test various natural language queries against the AI-powered search API

BASE_URL="http://localhost:8080"
API_ENDPOINT="$BASE_URL/api/ai/query"

echo "================================"
echo "AI Search API Testing"
echo "================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test query
test_query() {
    local query="$1"
    local description="$2"

    echo -e "${YELLOW}Test: $description${NC}"
    echo "Query: $query"
    echo ""

    response=$(curl -s -X POST "$API_ENDPOINT" \
        -H "Content-Type: application/json" \
        -d "{\"message\": \"$query\"}")

    echo -e "${GREEN}Response:${NC}"
    echo "$response" | jq '.'
    echo ""
    echo "---"
    echo ""
}

# Check if service is healthy
echo "Checking service health..."
health=$(curl -s "$BASE_URL/api/ai/health")
echo "$health" | jq '.'
echo ""
echo "---"
echo ""

# Test 1: Statistical Query (execute_sql)
test_query "How many commits are in the database?" "Statistical Query - Total Commits"

# Test 2: Author-specific count
test_query "How many commits did youssef make?" "Statistical Query - Commits by Author"

# Test 3: Semantic Search
test_query "Find commits about authentication" "Semantic Search - Authentication"

# Test 4: Semantic Search - Bug fixes
test_query "Show me bug fixes" "Semantic Search - Bug Fixes"

# Test 5: Specific commit
test_query "What changed in commit b173df9b767cdc4bc4b3823fe1cb2d663c16eb55?" "Retrieve Specific Commit"

# Test 6: Hybrid Search
test_query "Find youssef's commits about configuration changes" "Hybrid Search - Author + Topic"

# Test 7: Date-based query
test_query "Show me commits from last week" "SQL Query - Date Range"

# Test 8: Complex semantic search
test_query "Find database migration or schema changes" "Semantic Search - Database Changes"

# Test 9: Code quality query
test_query "Find commits with code quality improvements" "Semantic Search - Quality Improvements"

# Test 10: Recent activity
test_query "What was the most recent commit?" "SQL Query - Latest Commit"

echo "================================"
echo "Testing Complete"
echo "================================"

