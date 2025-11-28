package com.ntsal.ntsal_ai_knowledge_hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntsal.ntsal_ai_knowledge_hub.entity.ConfigsEntity;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Query Planner Service - Uses LLM to convert natural language to structured query plans
 */
@Service
public class QueryPlannerService {
    private final ConfigsService configsService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public QueryPlannerService(ConfigsService configsService) {
        this.configsService = configsService;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    /**
     * Plan a query based on natural language input
     * Returns a Plan object with action type and parameters
     */
    public Mono<Plan> plan(String question) {
        ConfigsEntity config = configsService.getLatestConfig();
        String apiUrl = config.getLlmSummarizerUrl();
        String apiKey = config.getLlmApiKey();
        String model = config.getLlmModel();

        String prompt = buildPlannerPrompt(question);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are a database query assistant. Respond ONLY with valid JSON."),
                        Map.of("role", "user", "content", prompt)
                },
                "temperature", 0.1,
                "max_tokens", 500
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
            var choices = (java.util.List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = message.get("content").toString().trim();

                // Extract JSON from markdown code blocks if present
                content = extractJsonFromResponse(content);

                Map<String, Object> planMap = objectMapper.readValue(content, Map.class);
                return Mono.just(Plan.fromMap(planMap));
            }
        } catch (Exception e) {
            System.err.println("QueryPlanner error: " + e.getMessage());
            e.printStackTrace();
            // Fallback to semantic search
            return Mono.just(new Plan("semantic_search", null, "summary_text", question, null));
        }

        return Mono.just(new Plan("semantic_search", null, "summary_text", question, null));
    }

    private String buildPlannerPrompt(String question) {
        return """
            You are a database & code assistant for a Git commit knowledge base.
            Convert the user's natural question into EXACT JSON describing ONE action.
            
            Database Schema:
            - Table: commit
              Columns: id, commit_hash, author, committed_date, message, diff_text, 
                       summary_text, feedback, embedding_vector, github_repo_id
              (NOTE: There is NO author_email column)
            - Table: github_repo
              Columns: id, repo_name, repo_url, owner
            - Table: configs
              Columns: id, config_key, config_value, created_at
            
            Allowed actions:
            1. "execute_sql" - For statistical queries, counting, filtering by date/author
               Example: {"action":"execute_sql","sql":"SELECT COUNT(*) FROM commit WHERE LOWER(author) LIKE '%%john%%'"}
            
            2. "semantic_search" - For finding commits by meaning/content/topic
               Example: {"action":"semantic_search","field":"summary_text","query":"authentication login feature"}
            
            3. "retrieve_commit" - For getting specific commit details by hash
               Example: {"action":"retrieve_commit","commit_hash":"abc123"}
            
            4. "hybrid_search" - Combine SQL filtering + semantic search
               Example: {"action":"hybrid_search","sql":"SELECT * FROM commit WHERE LOWER(author) LIKE '%%alice%%'","semantic_query":"bug fixes"}
            
            IMPORTANT Rules for Author Queries:
            - When filtering by author name, ONLY use: LOWER(author) LIKE '%%name%%'
            - This matches partial names case-insensitively (e.g., "john" matches "John Doe", "john", "johndoe")
            - Example: For "john", use: WHERE LOWER(author) LIKE '%%john%%'
            - There is NO author_email column, only 'author'
            - Never use exact match (author = 'name') unless explicitly requested
            
            IMPORTANT Rules for Date Queries:
            - Column name is "committed_date" (NOT commit_date)
            - For "last week": WHERE committed_date >= CURRENT_DATE - INTERVAL '7 days'
            - For "this month": WHERE committed_date >= DATE_TRUNC('month', CURRENT_DATE)
            - For "last month": WHERE committed_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month') AND committed_date < DATE_TRUNC('month', CURRENT_DATE)
            - For "today": WHERE committed_date >= CURRENT_DATE
            - For "recent" or "latest": ORDER BY committed_date DESC LIMIT 10
            - Always use PostgreSQL date functions: CURRENT_DATE, INTERVAL, DATE_TRUNC
            
            General Rules:
            - Respond ONLY with valid JSON, no explanations
            - Use execute_sql for: counts, statistics, date ranges, author filtering
            - Use semantic_search for: "find commits about X", "what changes related to Y"
            - Use retrieve_commit when user mentions a specific commit hash
            - Use hybrid_search when query needs both filtering and semantic matching
            
            User question: %s
            
            JSON response:
            """.formatted(question);
    }

    private String extractJsonFromResponse(String content) {
        // Remove markdown code blocks
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    /**
     * Plan record representing a structured query plan
     */
    public record Plan(String action, String sql, String field, String query, String commitHash) {
        public static Plan fromMap(Map<String, Object> map) {
            String action = (String) map.get("action");
            return new Plan(
                    action,
                    (String) map.getOrDefault("sql", null),
                    (String) map.getOrDefault("field", "summary_text"),
                    (String) map.getOrDefault("query", null),
                    (String) map.getOrDefault("commit_hash", null)
            );
        }
    }
}

