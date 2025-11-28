package com.ntsal.ntsal_ai_knowledge_hub.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP (Model Context Protocol) Client for interacting with PostgreSQL MCP server via SSE
 * This client enables AI-powered database queries through natural language
 */
@Component
public class McpClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public McpClient(@Value("${mcp.server.url:http://localhost:3000}") String mcpServerUrl,
                     @Value("${mcp.enabled:false}") boolean enabled) {
        this.enabled = enabled;
        this.webClient = WebClient.builder()
                .baseUrl(mcpServerUrl)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Execute SQL query through MCP server using MCP protocol format
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> executeSql(String sql) {
        if (!enabled) {
            return Mono.just(Map.of("error", "MCP is disabled"));
        }

        // MCP protocol format for tool calls
        Map<String, Object> payload = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", "mcp_postgres_execute_sql",
                        "arguments", Map.of("sql", sql)
                )
        );

        return (Mono<Map<String, Object>>) (Mono<?>) webClient.post()
                .uri("/message")  // SSE message endpoint
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    System.err.println("MCP executeSql error: " + e.getMessage());
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }

    /**
     * Get database schema information
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSchema() {
        if (!enabled) {
            return Mono.just(Map.of("error", "MCP is disabled"));
        }

        Map<String, Object> payload = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", "mcp_postgres_list_schemas",
                        "arguments", Map.of()
                )
        );

        return (Mono<Map<String, Object>>) (Mono<?>) webClient.post()
                .uri("/message")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    System.err.println("MCP getSchema error: " + e.getMessage());
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }

    /**
     * Get table details from database
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getTableDetails(String schemaName, String tableName) {
        if (!enabled) {
            return Mono.just(Map.of("error", "MCP is disabled"));
        }

        Map<String, Object> payload = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", "mcp_postgres_get_object_details",
                        "arguments", Map.of(
                                "schema_name", schemaName,
                                "object_name", tableName,
                                "object_type", "table"
                        )
                )
        );

        return (Mono<Map<String, Object>>) (Mono<?>) webClient.post()
                .uri("/message")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    System.err.println("MCP getTableDetails error: " + e.getMessage());
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }

    /**
     * Explain query execution plan (useless for now)
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> explainQuery(String sql) {
        if (!enabled) {
            return Mono.just(Map.of("error", "MCP is disabled"));
        }

        Map<String, Object> payload = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", "mcp_postgres_explain_query",
                        "arguments", Map.of("sql", sql, "analyze", false)
                )
        );

        return (Mono<Map<String, Object>>) (Mono<?>) webClient.post()
                .uri("/message")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    System.err.println("MCP explainQuery error: " + e.getMessage());
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }

    /**
     * Generic tool call for any MCP tool (useless for now)
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> callTool(String toolName, Map<String, Object> arguments) {
        if (!enabled) {
            return Mono.just(Map.of("error", "MCP is disabled"));
        }

        Map<String, Object> payload = Map.of(
                "method", "tools/call",
                "params", Map.of(
                        "name", toolName,
                        "arguments", arguments
                )
        );

        return (Mono<Map<String, Object>>) (Mono<?>) webClient.post()
                .uri("/message")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    System.err.println("MCP callTool(" + toolName + ") error: " + e.getMessage());
                    return Mono.just(Map.of("error", e.getMessage()));
                });
    }
}
