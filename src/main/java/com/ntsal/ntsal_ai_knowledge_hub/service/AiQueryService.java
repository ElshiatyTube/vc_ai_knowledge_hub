package com.ntsal.ntsal_ai_knowledge_hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntsal.ntsal_ai_knowledge_hub.client.EmbeddingClient;
import com.ntsal.ntsal_ai_knowledge_hub.entity.ConfigsEntity;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * AI Query Service - Main orchestrator for natural language queries
 * Combines LLM planning, MCP tools, semantic search, and answer generation
 */
@Service
public class AiQueryService {
    private final QueryPlannerService plannerService;
    private final SqlExecutorService sqlExecutorService;
    private final SemanticSearchService semanticSearchService;
    private final EmbeddingClient embeddingClient;
    private final ConfigsService configsService;
    private final CommitEmbeddingService commitEmbeddingService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiQueryService(QueryPlannerService plannerService,
                          SqlExecutorService sqlExecutorService,
                          SemanticSearchService semanticSearchService,
                          EmbeddingClient embeddingClient,
                          ConfigsService configsService,
                          CommitEmbeddingService commitEmbeddingService) {
        this.plannerService = plannerService;
        this.sqlExecutorService = sqlExecutorService;
        this.semanticSearchService = semanticSearchService;
        this.embeddingClient = embeddingClient;
        this.configsService = configsService;
        this.commitEmbeddingService = commitEmbeddingService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Main entry point: Ask a natural language question and get an AI-powered answer
     */
    public Mono<Map<String, Object>> ask(String question) {
        return plannerService.plan(question)
                .flatMap(plan -> executePlan(question, plan))
                .onErrorResume(error -> {
                    System.err.println("AiQueryService error: " + error.getMessage());
                    return Mono.just(Map.of(
                            "answer", "Sorry, I encountered an error processing your question: " + error.getMessage(),
                            "error", true
                    ));
                });
    }

    /**
     * Execute the planned action
     */
    private Mono<Map<String, Object>> executePlan(String question, QueryPlannerService.Plan plan) {
        System.out.println("Executing plan: " + plan.action());

        return switch (plan.action()) {
            case "execute_sql" -> executeSqlQuery(question, plan);
            case "semantic_search" -> executeSemanticSearch(question, plan);
            case "retrieve_commit" -> retrieveCommit(question, plan);
            case "hybrid_search" -> executeHybridSearch(question, plan);
            default -> Mono.just(Map.of(
                    "answer", "Unsupported action: " + plan.action(),
                    "error", true
            ));
        };
    }

    /**
     * Execute SQL query via MCP and generate natural language answer
     */
    private Mono<Map<String, Object>> executeSqlQuery(String question, QueryPlannerService.Plan plan) {
        if (plan.sql() == null || plan.sql().isEmpty()) {
            return Mono.just(Map.of("answer", "No SQL query provided", "error", true));
        }

        // Validate SQL safety
        if (!sqlExecutorService.isSafeSql(plan.sql())) {
            return Mono.just(Map.of(
                    "answer", "Query rejected for safety reasons. Only SELECT queries are allowed.",
                    "error", true
            ));
        }

        return sqlExecutorService.executeSql(plan.sql())
                .flatMap(sqlResults -> generateNaturalAnswer(question, sqlResults, "sql"));
    }

    /**
     * Execute semantic search using embeddings
     */
    private Mono<Map<String, Object>> executeSemanticSearch(String question, QueryPlannerService.Plan plan) {
        try {
            String searchQuery = plan.query() != null ? plan.query() : question;

            // Generate embedding for the search query
            float[] queryEmbedding = embeddingClient.generateEmbedding(searchQuery);

            if (queryEmbedding == null || queryEmbedding.length == 0) {
                return Mono.just(Map.of("answer", "Failed to generate embedding for the query", "error", true));
            }

            // Perform semantic search
            List<Map<String, Object>> results = semanticSearchService.searchByEmbedding(queryEmbedding, 10);

            // Format results for LLM
            String formattedResults = semanticSearchService.formatSearchResults(results);

            // Generate natural language answer
            return generateNaturalAnswer(question, formattedResults, "semantic", results);

        } catch (Exception e) {
            System.err.println("Semantic search error: " + e.getMessage());
            return Mono.just(Map.of(
                    "answer", "Error performing semantic search: " + e.getMessage(),
                    "error", true
            ));
        }
    }

    /**
     * Retrieve specific commit by hash
     */
    private Mono<Map<String, Object>> retrieveCommit(String question, QueryPlannerService.Plan plan) {
        if (plan.commitHash() == null || plan.commitHash().isEmpty()) {
            return Mono.just(Map.of("answer", "No commit hash provided", "error", true));
        }

        String sql = String.format(
                "SELECT commit_hash, author, committed_date, message, summary_text, feedback, diff_text " +
                        "FROM commit WHERE commit_hash LIKE '%s%%' LIMIT 1",
                plan.commitHash()
        );

        return sqlExecutorService.executeSql(sql)
                .flatMap(results -> generateNaturalAnswer(question, results, "commit"));
    }

    /**
     * Execute hybrid search (SQL filtering + semantic search)
     */
    private Mono<Map<String, Object>> executeHybridSearch(String question, QueryPlannerService.Plan plan) {
        try {
            String searchQuery = plan.query() != null ? plan.query() : question;

            // Generate embedding
            float[] queryEmbedding = embeddingClient.generateEmbedding(searchQuery);

            if (queryEmbedding == null || queryEmbedding.length == 0) {
                return Mono.just(Map.of("answer", "Failed to generate embedding", "error", true));
            }

            // Perform hybrid search
            List<Map<String, Object>> results = plan.sql() != null ?
                    semanticSearchService.hybridSearch(plan.sql(), queryEmbedding, 20) :
                    semanticSearchService.searchByEmbedding(queryEmbedding, 20);

            String formattedResults = semanticSearchService.formatSearchResults(results);

            return generateNaturalAnswer(question, formattedResults, "hybrid", results);

        } catch (Exception e) {
            System.err.println("Hybrid search error: " + e.getMessage());
            return Mono.just(Map.of(
                    "answer", "Error performing hybrid search: " + e.getMessage(),
                    "error", true
            ));
        }
    }

    /**
     * Generate natural language answer using LLM
     */
    private Mono<Map<String, Object>> generateNaturalAnswer(String question, String data, String sourceType) {
        return generateNaturalAnswer(question, data, sourceType, null);
    }

    private Mono<Map<String, Object>> generateNaturalAnswer(String question, String data, String sourceType, List<Map<String, Object>> sourceData) {
        ConfigsEntity config = configsService.getLatestConfig();
        String apiUrl = config.getLlmSummarizerUrl();
        String apiKey = config.getLlmApiKey();
        String model = config.getLlmModel();

        String prompt = buildAnswerPrompt(question, data, sourceType);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are a helpful AI assistant that provides clear, concise answers based on database results."),
                        Map.of("role", "user", "content", prompt)
                },
                "temperature", 0.3,
                "max_tokens", 800
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
                String answer = message.get("content").toString().trim();

                Map<String, Object> result = new HashMap<>();
                result.put("answer", answer);
                result.put("source_type", sourceType);

                // Add sources for semantic/hybrid search
                if (sourceData != null && !sourceData.isEmpty()) {
                    List<Map<String, Object>> sources = new ArrayList<>();
                    for (int i = 0; i < Math.min(5, sourceData.size()); i++) {
                        Map<String, Object> item = sourceData.get(i);
                        sources.add(Map.of(
                                "commit_hash", item.get("commit_hash"),
                                "author", item.get("author"),
                                "score", item.get("score")
                        ));
                    }
                    result.put("sources", sources);
                }

                return Mono.just(result);
            }
        } catch (Exception e) {
            System.err.println("Answer generation error: " + e.getMessage());
        }

        return Mono.just(Map.of(
                "answer", "I found some information but couldn't generate a proper answer. Here's the raw data:\n\n" + data,
                "source_type", sourceType
        ));
    }

    /**
     * Build prompt for answer generation
     */
    private String buildAnswerPrompt(String question, String data, String sourceType) {
        return switch (sourceType) {
            case "sql" -> """
                You are analyzing database query results to answer a user's question.
                
                User Question: %s
                
                Database Results:
                %s
                
                Task: Provide a clear, concise answer (2-4 sentences) based on the data above.
                If the data shows statistics or counts, present them clearly.
                If no relevant data is found, say so directly.
                """.formatted(question, data);

            case "semantic", "hybrid" -> """
                You are analyzing Git commit history to answer a user's question.
                
                User Question: %s
                
                Relevant Commits Found:
                %s
                
                Task: 
                1. Provide a clear answer (2-4 sentences) summarizing the relevant commits
                2. Highlight the most relevant commit(s) that answer the question
                3. Mention key details like authors, dates, or changes if relevant
                4. Be specific and reference commit hashes when discussing specific changes
                """.formatted(question, data);

            case "commit" -> """
                You are explaining a specific Git commit to a user.
                
                User Question: %s
                
                Commit Details:
                %s
                
                Task: Provide a comprehensive explanation of this commit including:
                - What changes were made
                - Why they might have been made (if summary/feedback provides insight)
                - Any code quality feedback or concerns
                - Keep the explanation clear and developer-friendly
                """.formatted(question, data);

            default -> """
                Question: %s
                Data: %s
                
                Provide a clear, helpful answer based on the information above.
                """.formatted(question, data);
        };
    }
}

