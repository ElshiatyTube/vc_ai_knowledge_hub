package com.ntsal.ntsal_ai_knowledge_hub.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic Search Service - Performs vector similarity search using pgvector
 */
@Service
public class SemanticSearchService {
    private final JdbcTemplate jdbcTemplate;

    public SemanticSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Search commits by embedding vector similarity
     *
     * @param queryEmbedding The query embedding as float array
     * @param limit Maximum number of results
     * @return List of matching commits with similarity scores
     */
    public List<Map<String, Object>> searchByEmbedding(float[] queryEmbedding, int limit) {
        String vectorString = floatArrayToVectorString(queryEmbedding);

        // Use direct string formatting for both vector and limit to avoid JDBC issues
        String sql = """
            SELECT 
                id, 
                commit_hash, 
                author,
                committed_date,
                message,
                summary_text, 
                feedback,
                1 - (embedding_vector <=> '%s'::vector) AS score
            FROM commit
            WHERE embedding_vector IS NOT NULL
            ORDER BY embedding_vector <=> '%s'::vector
            LIMIT %d
        """.formatted(vectorString, vectorString, limit);

        return jdbcTemplate.query(sql,
            (rs, rowNum) -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("commit_hash", rs.getString("commit_hash"));
                row.put("author", rs.getString("author"));
                row.put("committed_date", rs.getTimestamp("committed_date"));
                row.put("message", rs.getString("message"));
                row.put("summary_text", rs.getString("summary_text"));
                row.put("feedback", rs.getString("feedback"));
                row.put("score", rs.getDouble("score"));
                return row;
            }
        );
    }

    /**
     * Hybrid search: SQL filtering + semantic search
     */
    public List<Map<String, Object>> hybridSearch(String sqlFilter, float[] queryEmbedding, int limit) {
        String vectorString = floatArrayToVectorString(queryEmbedding);

        // Build dynamic query with WHERE clause from sqlFilter
        String sql = """
            SELECT 
                id, 
                commit_hash, 
                author,
                committed_date,
                message,
                summary_text,
                feedback,
                1 - (embedding_vector <=> '%s'::vector) AS score
            FROM commit
            WHERE embedding_vector IS NOT NULL
            AND (%s)
            ORDER BY embedding_vector <=> '%s'::vector
            LIMIT %d
        """.formatted(vectorString, extractWhereClause(sqlFilter), vectorString, limit);

        try {
            return jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("commit_hash", rs.getString("commit_hash"));
                    row.put("author", rs.getString("author"));
                    row.put("committed_date", rs.getTimestamp("committed_date"));
                    row.put("message", rs.getString("message"));
                    row.put("summary_text", rs.getString("summary_text"));
                    row.put("feedback", rs.getString("feedback"));
                    row.put("score", rs.getDouble("score"));
                    return row;
                }
            );
        } catch (Exception e) {
            System.err.println("Hybrid search error: " + e.getMessage());
            // Fallback to regular semantic search
            return searchByEmbedding(queryEmbedding, limit);
        }
    }

    /**
     * Search by specific field (summary_text or diff_text)
     */
    public List<Map<String, Object>> searchByField(String field, float[] queryEmbedding, int limit) {
        // For now, we only have embedding_vector on the commit table
        // This can be extended if we add separate embeddings for diff_text
        return searchByEmbedding(queryEmbedding, limit);
    }

    /**
     * Convert float array to PostgreSQL vector string format
     */
    private String floatArrayToVectorString(float[] array) {
        if (array == null || array.length == 0) {
            return "[0]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Extract WHERE clause from SQL query
     */
    private String extractWhereClause(String sql) {
        String upperSql = sql.toUpperCase();
        int whereIndex = upperSql.indexOf("WHERE");

        if (whereIndex == -1) {
            return "1=1"; // No WHERE clause, return always true
        }

        String whereClause = sql.substring(whereIndex + 5).trim();

        // Remove ORDER BY, LIMIT, etc.
        int orderByIndex = whereClause.toUpperCase().indexOf("ORDER BY");
        if (orderByIndex != -1) {
            whereClause = whereClause.substring(0, orderByIndex).trim();
        }

        int limitIndex = whereClause.toUpperCase().indexOf("LIMIT");
        if (limitIndex != -1) {
            whereClause = whereClause.substring(0, limitIndex).trim();
        }

        return whereClause.isEmpty() ? "1=1" : whereClause;
    }

    /**
     * Format search results into readable text for LLM
     */
    public String formatSearchResults(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return "No results found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" relevant commits:\n\n");

        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> row = results.get(i);
            sb.append(String.format("[%d] Commit: %s\n", i + 1, row.get("commit_hash")));
            sb.append(String.format("    Author: %s\n", row.get("author")));
            sb.append(String.format("    Date: %s\n", row.get("commit_date")));
            sb.append(String.format("    Message: %s\n", row.get("message")));
            sb.append(String.format("    Summary: %s\n", row.get("summary_text")));

            if (row.get("feedback") != null && !row.get("feedback").toString().isEmpty()) {
                sb.append(String.format("    Feedback: %s\n", row.get("feedback")));
            }

            sb.append(String.format("    Relevance Score: %.3f\n\n", row.get("score")));
        }

        return sb.toString();
    }
}

