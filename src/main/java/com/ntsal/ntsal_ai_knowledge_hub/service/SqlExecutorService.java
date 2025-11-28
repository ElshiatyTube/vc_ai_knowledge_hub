package com.ntsal.ntsal_ai_knowledge_hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntsal.ntsal_ai_knowledge_hub.client.McpClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * SQL Executor Service - Executes SQL queries via MCP or direct JDBC
 */
@Service
public class SqlExecutorService {
    private final McpClient mcpClient;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SqlExecutorService(McpClient mcpClient, JdbcTemplate jdbcTemplate) {
        this.mcpClient = mcpClient;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Execute SQL query and return formatted results
     */
    public Mono<String> executeSql(String sql) {
        return mcpClient.executeSql(sql)
                .map(response -> {
                    try {
                        if (response.containsKey("error")) {
                            // Fallback to direct JDBC if MCP fails
                            return executeDirectJdbc(sql);
                        }

                        // Format the response for LLM consumption
                        return formatSqlResults(response);
                    } catch (Exception e) {
                        // Fallback to direct JDBC
                        return executeDirectJdbc(sql);
                    }
                });
    }

    /**
     * Direct JDBC execution as fallback
     */
    private String executeDirectJdbc(String sql) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            if (results.isEmpty()) {
                return "No results found.";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(results.size(), 50); i++) {
                Map<String, Object> row = results.get(i);
                sb.append("Row ").append(i + 1).append(": ");
                row.forEach((key, value) -> sb.append(key).append("=").append(value).append(", "));
                if (sb.length() > 2) sb.setLength(sb.length() - 2);
                sb.append("\n");
            }

            if (results.size() > 50) {
                sb.append("... (").append(results.size() - 50).append(" more rows)\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error executing SQL: " + e.getMessage();
        }
    }

    /**
     * Format SQL results into human-readable text
     */
    private String formatSqlResults(Map<String, Object> response) {
        try {
            StringBuilder result = new StringBuilder();

            // Check for different response formats from MCP
            if (response.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                for (Map<String, Object> item : content) {
                    if (item.containsKey("text")) {
                        result.append(item.get("text")).append("\n");
                    }
                }
            } else if (response.containsKey("result")) {
                Object resultData = response.get("result");
                if (resultData instanceof List) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) resultData;
                    if (rows.isEmpty()) {
                        return "No results found.";
                    }

                    // Format as table-like text
                    for (int i = 0; i < rows.size() && i < 50; i++) { // Limit to 50 rows
                        Map<String, Object> row = rows.get(i);
                        result.append("Row ").append(i + 1).append(": ");
                        row.forEach((key, value) ->
                            result.append(key).append("=").append(value).append(", ")
                        );
                        result.setLength(result.length() - 2); // Remove last comma
                        result.append("\n");
                    }

                    if (rows.size() > 50) {
                        result.append("... (").append(rows.size() - 50).append(" more rows)\n");
                    }
                } else {
                    result.append(resultData.toString());
                }
            } else {
                // Fallback: just stringify the whole response
                result.append(objectMapper.writeValueAsString(response));
            }

            return result.toString();
        } catch (Exception e) {
            return "Error formatting results: " + e.getMessage();
        }
    }

    /**
     * Validate SQL query for safety (prevent destructive operations)
     */
    public boolean isSafeSql(String sql) {
        String upperSql = sql.toUpperCase().trim();

        // Reject destructive operations
        String[] dangerousKeywords = {"DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE"};
        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                return false;
            }
        }

        // Must start with SELECT
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH")) {
            return false;
        }

        return true;
    }
}
