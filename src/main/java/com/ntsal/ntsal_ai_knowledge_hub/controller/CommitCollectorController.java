package com.ntsal.ntsal_ai_knowledge_hub.controller;

import com.ntsal.ntsal_ai_knowledge_hub.service.CommitCollectorService;
import com.ntsal.ntsal_ai_knowledge_hub.entity.CommitEntity;
import com.ntsal.ntsal_ai_knowledge_hub.service.CommitEmbeddingService;
import com.ntsal.ntsal_ai_knowledge_hub.service.CommitSummarizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commit")
public class CommitCollectorController {

    private final CommitCollectorService commitCollectorService;
    private final CommitSummarizerService summarizerService;
    private final CommitEmbeddingService embeddingService;
    @Autowired
    public CommitCollectorController(CommitCollectorService commitCollectorService, CommitSummarizerService summarizerService,
                                     CommitEmbeddingService embeddingService) {
        this.commitCollectorService = commitCollectorService;
        this.summarizerService = summarizerService;
        this.embeddingService = embeddingService;
    }

    // Collect commits manually
    @PostMapping("/collect")
    public ResponseEntity<String> collectCommits() {
        try {
            commitCollectorService.collectCommits();
            return ResponseEntity.ok("Commits collected successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error collecting commits: " + e.getMessage());
        }
    }

    //Generate embeddings manually
    @PostMapping("/generateEmbeddings")
    public ResponseEntity<String> generateEmbeddings() {
        try {
            embeddingService.generateEmbeddingsAsync();
            return ResponseEntity.ok("Embeddings generation started in background.");
        }catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating embeddings: " + e.getMessage());
        }
    }

    // Find all collected commits
    @GetMapping
    public ResponseEntity<List<CommitEntity>> findAllCommits() {
        try {
            List<CommitEntity> commits = commitCollectorService.findAll();
            return ResponseEntity.ok(commits);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Manually summarize new commits
    @PostMapping("/summarize")
    public String summarizeNow() {
        summarizerService.summarizeNewCommits();
        return "Summarization started!";
    }

    // Search commits by embedding vector
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String query) {
        return embeddingService.search(query);
    }
}
