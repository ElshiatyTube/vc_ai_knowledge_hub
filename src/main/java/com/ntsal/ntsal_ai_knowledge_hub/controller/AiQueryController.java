package com.ntsal.ntsal_ai_knowledge_hub.controller;

import com.ntsal.ntsal_ai_knowledge_hub.service.AiQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI Query Controller - Natural language interface for querying commit knowledge base
 */
@RestController
@RequestMapping("/api/ai")
public class AiQueryController {
    private final AiQueryService aiQueryService;

    public AiQueryController(AiQueryService aiQueryService) {
        this.aiQueryService = aiQueryService;
    }

    /**
     * Main AI query endpoint
     * Accepts natural language questions and returns intelligent answers
     *
     * Example requests:
     * - "How many commits did John make this month?"
     * - "Find commits about authentication"
     * - "What changed in commit abc123?"
     * - "Show me bug fixes by Alice"
     */
    @PostMapping("/query")
    public Mono<ResponseEntity<Map<String, Object>>> query(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty")));
        }

        return aiQueryService.ask(message)
                .map(ResponseEntity::ok)
                .onErrorResume(error ->
                    Mono.just(ResponseEntity.internalServerError()
                            .body(Map.of(
                                    "error", error.getMessage(),
                                    "answer", "An error occurred while processing your query."
                            )))
                );
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "AI Query Service",
                "features", "Natural Language Search, SQL Queries, Semantic Search"
        ));
    }

    /**
     * Get supported query types
     */
    @GetMapping("/query-types")
    public ResponseEntity<Map<String, Object>> getQueryTypes() {
        return ResponseEntity.ok(Map.of(
                "query_types", new String[]{
                        "execute_sql - Statistical queries, counts, filters",
                        "semantic_search - Find commits by meaning/topic",
                        "retrieve_commit - Get specific commit details",
                        "hybrid_search - Combined SQL + semantic search"
                },
                "examples", Map.of(
                        "sql", "How many commits were made last week?",
                        "semantic", "Find commits about login authentication",
                        "commit", "What changed in commit abc123?",
                        "hybrid", "Show me John's commits about bug fixes"
                )
        ));
    }
}

