package com.ntsal.ntsal_ai_knowledge_hub.service;

import com.ntsal.ntsal_ai_knowledge_hub.client.LLMClient;
import com.ntsal.ntsal_ai_knowledge_hub.entity.CommitEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommitSummarizerService {

    private final CommitCollectorService commitCollectorService;
    private final LLMClient lLMClient;

    public CommitSummarizerService(CommitCollectorService commitCollectorService, LLMClient lLMClient) {
        this.commitCollectorService = commitCollectorService;
        this.lLMClient = lLMClient;
    }
    // Run every day at 3 AM (after commit collector invoked)
    @Scheduled(cron = "0 0 3 * * *")
    public void summarizeNewCommits(){
        generateSummarizeNewCommits();
    }
    //@Async
    public void generateSummarizeNewCommits(){
        List<CommitEntity> unsummarized = commitCollectorService.findAll()
                .stream()
                .filter(c -> (c.getSummaryText() == null || c.getSummaryText().isEmpty()) ||
                             (c.getFeedback() == null || c.getFeedback().isEmpty()))
                .toList();

        System.out.println("🧠 Summarizing " + unsummarized.size() + " commits...");

        for (CommitEntity commit : unsummarized) {
            try {
                // Generate summary if missing
                if (commit.getSummaryText() == null || commit.getSummaryText().isEmpty()) {
                    String summary = lLMClient.summarizeCommit(commit.getMessage(), commit.getDiffText());
                    commit.setSummaryText(summary);
                    System.out.println("✅ Summary generated for commit " + commit.getCommitHash());
                    Thread.sleep(1500); // To avoid rate limit exceeded error
                }

                // Generate feedback if missing
                if (commit.getFeedback() == null || commit.getFeedback().isEmpty()) {
                    String feedback = lLMClient.generateFeedback(commit.getMessage(), commit.getDiffText());
                    commit.setFeedback(feedback);
                    System.out.println("✅ Feedback generated for commit " + commit.getCommitHash());
                    Thread.sleep(1500); // To avoid rate limit exceeded error
                }

                commitCollectorService.save(commit);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("🎉 All summaries and feedback generated successfully.");
    }
}
