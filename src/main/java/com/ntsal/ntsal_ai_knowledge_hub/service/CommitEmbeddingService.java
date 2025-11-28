package com.ntsal.ntsal_ai_knowledge_hub.service;

import com.ntsal.ntsal_ai_knowledge_hub.client.EmbeddingClient;
import com.ntsal.ntsal_ai_knowledge_hub.entity.CommitEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommitEmbeddingService {
    private final CommitCollectorService commitCollectorService;
    private final EmbeddingClient embeddingClient;

    public CommitEmbeddingService(CommitCollectorService commitCollectorService, EmbeddingClient embeddingClient) {
        this.commitCollectorService = commitCollectorService;
        this.embeddingClient = embeddingClient;
    }

    // Runs every day at 4 AM (after collection and summarization complete)
    @Scheduled(cron = "0 0 4 * * *")
    public void generateEmbeddings() {
        generateEmbeddingsAsync();
    }

    // Public async method that can be called from controller
   // @Async
    public void generateEmbeddingsAsync() {
        List<CommitEntity> commits = commitCollectorService.findAll()
                .stream()
                .filter(c -> c.getSummaryText() != null &&
                             c.getFeedback() != null &&
                             c.getEmbeddingVector() == null)
                .toList();

        System.out.println("🧬 Generating embeddings for " + commits.size() + " commits...");

        for (CommitEntity commit : commits) {
            try {
                // Combine summary and feedback for richer semantic embeddings
                String combinedText = String.format("""
                    Summary: %s
                    
                    Feedback: %s
                    """,
                    commit.getSummaryText(),
                    commit.getFeedback());

                float[] embedding = embeddingClient.generateEmbedding(combinedText);
                commit.setEmbeddingVector(new com.pgvector.PGvector(Arrays.toString(embedding)));
                commitCollectorService.save(commit);
                System.out.println("✅ Embedding generated for commit " + commit.getCommitHash());
                Thread.sleep(1500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("🎉 Embedding generation complete!");
    }

    public Map<String, Object> search(String query) {
        float[] queryEmbedding = embeddingClient.generateEmbedding(query);

        // Convert float[] to String for the query
        String embeddingString = Arrays.toString(queryEmbedding);

        // Use the String representation for the search
        List<CommitEntity> results = commitCollectorService.searchByEmbedding(embeddingString);
        System.out.println("🔍 Found " + results.size() + " relevant commits for the query.");
        // Map results to the expected return type
        List<Map<String, Object>> commits = results.stream()
                .map(c -> Map.<String, Object>of(
                        "hash", c.getCommitHash(),
                        "author", c.getAuthor(),
                        "message", c.getMessage(),
                        "summary", (c.getSummaryText() != null ? c.getSummaryText() : "No summary available"),
                        "feedback", (c.getFeedback() != null ? c.getFeedback() : "No feedback available"),
                        "committedDate", c.getCommittedDate().toString(),
                        "repoName", (c.getGithubRepo() != null ? c.getGithubRepo().getName() : "Unknown")
                )).toList();

        // Return commits without AI analysis for faster response
        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("commits", commits);
        response.put("totalResults", commits.size());

        return response;
    }

}
