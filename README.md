# 🚀 VC AI Knowledge Hub

AI-powered commit knowledge search system with natural language queries, semantic search, and intelligent code analysis.

## ✨ Features

- 🤖 **Natural Language Queries** - Ask questions in plain text
- 🔍 **Semantic Search** - Find commits by meaning, not just keywords
- 📊 **Smart SQL Generation** - LLM converts questions to optimized SQL
- 💡 **Code Intelligence** - Automated summarization and feedback
- 📈 **Vector Similarity** - pgvector for fast semantic search
- 🐳 **Docker Ready** - Complete containerized deployment

## 🏗️ Architecture for chatbot

```
User Question → LLM Planner → SQL/Semantic Search → LLM Answer Generator → Response
                     ↓              ↓
                PostgreSQL    pgvector (384-dim)
                (with data)   (embeddings)
```

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- 4GB+ RAM
- GitHub token (for repo access)
- OpenAI/DeepSeek API key

### 1. Setup

```bash
# Clone the repository
git clone https://github.com/ElshiatyTube/vc_ai_knowledge_hub.git
cd vc_ai_knowledge_hub

# Configure environment
cp .env.example .env
# Edit .env with your API keys and configuration
```

### 2. Deploy

```bash
# Build the project
mvn clean package -DskipTests

# Start all services
docker-compose -f docker-compose-prebuilt.yml up -d
```

### 3. Test

```bash
# Health check
curl http://localhost:8080/api/ai/health

# Ask a question
curl -X POST http://localhost:8080/api/ai/query \
  -H "Content-Type: application/json" \
  -d '{"message": "How many commits did John make?"}'
```

## 📚 API Reference

### Natural Language Query
```bash
POST /api/ai/query
Content-Type: application/json

{
  "message": "Find commits about authentication"
}
```

**Response:**
```json
{
  "answer": "I found 3 relevant commits about authentication...",
  "source_type": "semantic",
  "sources": [
    {"commit_hash": "abc123", "score": 0.92}
  ]
}
```

### Example Queries

| Query | Type | Description |
|-------|------|-------------|
| "Give me feedback about John's work." | Hybrid | SQL filter + semantic search
| "How many commits?" | SQL | Total count |
| "How many commits did John make?" | SQL | Author-specific with fuzzy matching |
| "Find commits about authentication" | Semantic | Meaning-based search |
| "What changed in commit abc123?" | Retrieval | Specific commit details |
| "Show me John's bug fixes" | Hybrid | SQL filter + semantic search |
| "Commits from last week" | SQL | Date-based queries |


## 🎯 Key Components

### 1. Query Planner (LLM)
- Converts natural language → structured plan
- Chooses optimal action (SQL, semantic, hybrid)
- Uses schema-aware prompts

### 2. SQL Executor
- Direct JDBC execution
- Safety validation (read-only)
- Fuzzy author matching

### 3. Semantic Search
- pgvector similarity search
- 384-dimensional embeddings
- Cosine distance ranking

### 4. Answer Generator (LLM)
- Converts results → natural language
- Includes source attribution
- Provides relevance scores

## 🗄️ Database Schema

```sql
commit (
  id              BIGSERIAL PRIMARY KEY,
  commit_hash     VARCHAR NOT NULL,
  author          VARCHAR NOT NULL,
  message         VARCHAR NOT NULL,
  diff_text       TEXT NOT NULL,
  summary_text    TEXT,
  feedback        TEXT,
  embedding_vector VECTOR(384),
  committed_date  TIMESTAMP NOT NULL,
  github_repo_id  BIGINT REFERENCES github_repo(id)
)
```

## 🔧 Configuration

### Environment Variables

```bash
# Optional
POSTGRES_PASSWORD=root                   # Database password
APP_PORT=8080                           # Application port
EMBEDDING_PORT=8000                     # Embedding service port
```

### Services

| Service | Default Port | Configurable |
|---------|--------------|--------------|
| Spring Boot API | 8080 | `APP_PORT` |
| PostgreSQL | 5432 | `POSTGRES_PORT` |
| Embedding Service | 8000 | `EMBEDDING_PORT` |
| MCP Server (opt) | 3000 | `MCP_PORT` |

## 📊 Performance

- **Query Response Time:** 1-2 seconds
- **Semantic Search:** ~100ms (pgvector)
- **SQL Queries:** ~50ms (direct JDBC)
- **Embedding Generation:** ~200ms

### Optimization Tips (TODOS)

```sql
-- Add indexes for better performance
CREATE INDEX idx_commit_hash ON commit(commit_hash);
CREATE INDEX idx_commit_author ON commit(author);
CREATE INDEX idx_commit_date ON commit(committed_date DESC);

-- pgvector index
CREATE INDEX idx_commit_embedding 
  ON commit USING ivfflat (embedding_vector vector_cosine_ops)
  WITH (lists = 100);
```

## 🐳 Docker Services

### PostgreSQL with pgvector
- **Image:** `ankane/pgvector:latest`
- **Purpose:** Database with vector search
- **Volume:** Persistent data storage

### Spring Boot Application
- **Build:** Multi-stage Maven build
- **Runtime:** Java 21 JRE Alpine
- **Health:** `/api/ai/health`

### Embedding Service
- **Model:** `all-MiniLM-L6-v2`
- **Framework:** Sentence Transformers
- **Output:** 384-dimensional vectors

### PostgreSQL MCP server: https://github.com/crystaldba/postgres-mcp

## 🛠️ Development

### Local Development

```bash
# Start dependencies only
docker-compose up -d postgres embedding_service

# Run app locally
mvn spring-boot:run
```

### Testing

```bash
# Run full test suite
./test_ai_search.sh

# Manual test
curl -X POST http://localhost:8080/api/ai/query \
  -H "Content-Type: application/json" \
  -d '{"message": "test query"}'
```

## 📝 Maintenance

### Restart Services
```bash
docker-compose restart app          # Restart app
docker-compose restart              # Restart all
./deploy.sh                        # Full redeploy
```

### Backup Database
```bash
# Backup to SQL file
docker-compose exec postgres pg_dump -U postgres vc_ai_knowledge_hub > backup.sql

# Restore from backup
cat backup.sql | docker-compose exec -T postgres psql -U postgres vc_ai_knowledge_hub
```

### Stop Services
```bash
./stop.sh                          # Stop all
docker-compose down                # Stop and remove containers
docker-compose down -v             # Stop and remove volumes (⚠️ deletes data)
```

## 🔒 Security

- ✅ Non-root container user
- ✅ Environment variable secrets
- ✅ SQL injection protection
- ✅ Read-only SQL validation
- ✅ Health checks enabled
- ✅ Resource limits configured


## 📊 Monitoring

```bash
# Container stats
docker stats

# Check health
curl http://localhost:8080/api/ai/health
curl http://localhost:8000/health

# Database connections
docker-compose exec postgres psql -U postgres -c "SELECT count(*) FROM pg_stat_activity"
```

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [pgvector](https://github.com/pgvector/pgvector) for vector similarity search
- [Sentence Transformers](https://www.sbert.net/) for embeddings
- [DeepSeek](https://www.deepseek.com/) for LLM capabilities
- [Spring Boot](https://spring.io/projects/spring-boot) ecosystem

## 🌟 Star History

If you find this project useful, please consider giving it a star! ⭐

## 📮 Contact

For questions or suggestions, please open an issue on GitHub.

---

**Built with ❤️ for the AI community**

---

**Status:** ✅ Production Ready


