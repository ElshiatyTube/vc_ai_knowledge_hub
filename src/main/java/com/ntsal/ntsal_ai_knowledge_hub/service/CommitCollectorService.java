package com.ntsal.ntsal_ai_knowledge_hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntsal.ntsal_ai_knowledge_hub.client.GitHubClient;
import com.ntsal.ntsal_ai_knowledge_hub.entity.CommitEntity;
import com.ntsal.ntsal_ai_knowledge_hub.entity.GithubRepoEntity;
import com.ntsal.ntsal_ai_knowledge_hub.repo.CommitRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommitCollectorService {

    private final GitHubClient gitHubClient;
    private final CommitRepository commitRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CommitCollectorService(GitHubClient gitHubClient, CommitRepository commitRepository) {
        this.gitHubClient = gitHubClient;
        this.commitRepository = commitRepository;
    }
    // Every day at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void collectCommits(){
        collectNewCommitsAsync();
    }
    //@Async
    public void collectNewCommitsAsync() {
        try {
            var config = gitHubClient.getLatestConfig();
            String token = config.getGithubToken();
            List<GithubRepoEntity> repos = gitHubClient.getAllRepos();
            for (GithubRepoEntity repo : repos) {
                String repoOwner = repo.getOwner();
                String repoName = repo.getName();
                String branch = repo.getBranch() != null ? repo.getBranch() : "main";
                var response = gitHubClient.fetchCommits(repoOwner, repoName, token, branch);
                JsonNode commitsArray = objectMapper.readTree(response.getBody());
                for (JsonNode commitNode : commitsArray) {
                    String sha = commitNode.get("sha").asText();
                    if (commitRepository.existsByCommitHash(sha)) continue;

                    String author = commitNode.path("commit").path("author").path("name").asText();
                    String message = commitNode.path("commit").path("message").asText();
                    String date = commitNode.path("commit").path("author").path("date").asText();

                    var diffResponse = gitHubClient.fetchCommitDiff(repoOwner, repoName, sha, token);
                    String diff = diffResponse.getBody();

                    CommitEntity entity = new CommitEntity();
                    entity.setCommitHash(sha);
                    entity.setAuthor(author);
                    entity.setMessage(message);
                    entity.setDiffText(diff);
                    entity.setCommittedDate(LocalDateTime.parse(date.replace("Z", "")));
                    entity.setGithubRepo(repo);

                    commitRepository.save(entity);
                }
            }
            System.out.println("✅ Commits collected successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<CommitEntity> findAll() {
        return commitRepository.findAll();
    }
    public void save(CommitEntity commit) {
        commitRepository.save(commit);
    }
    public List<CommitEntity> searchByEmbedding(String queryEmbedding) {
        return commitRepository.searchByEmbedding(queryEmbedding);
    }
}
