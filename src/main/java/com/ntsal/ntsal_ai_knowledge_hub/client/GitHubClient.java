package com.ntsal.ntsal_ai_knowledge_hub.client;

import com.ntsal.ntsal_ai_knowledge_hub.entity.ConfigsEntity;
import com.ntsal.ntsal_ai_knowledge_hub.entity.GithubRepoEntity;
import com.ntsal.ntsal_ai_knowledge_hub.service.ConfigsService;
import com.ntsal.ntsal_ai_knowledge_hub.service.GithubRepoService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GitHubClient {
    private final ConfigsService configsService;
    private final GithubRepoService githubRepoService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GitHubClient(ConfigsService configsService, GithubRepoService githubRepoService) {
        this.configsService = configsService;
        this.githubRepoService = githubRepoService;
    }

    /**
     * Fetch commits for all unique GitHub repos.
     * @return List of ResponseEntity for each repo
     */
    public List<ResponseEntity<String>> fetchCommitsForAllRepos() {
        ConfigsEntity config = configsService.getLatestConfig();
        String token = config.getGithubToken();
        List<GithubRepoEntity> repos = githubRepoService.findAll();
        Set<Map<String,String>> uniqueRepos = repos.stream()
                .map(githubRepoEntity -> Map.of(
                        "name", githubRepoEntity.getName(),
                        "owner", githubRepoEntity.getOwner(),
                        "branch", githubRepoEntity.getBranch() != null ? githubRepoEntity.getBranch() : "main"
                ))
                .filter(map -> map.get("name") != null && !map.get("name").isBlank()
                        && map.get("owner") != null && !map.get("owner").isBlank())
                .collect(Collectors.toSet());
        if (uniqueRepos.isEmpty()) throw new IllegalStateException("No GitHub repos configured");
        return uniqueRepos.stream()
                .map(map -> fetchCommits(map.get("owner"), map.get("name"), token, map.get("branch")))
                .collect(Collectors.toList());
    }

    /**
     * Fetch commits for a specific repo name and branch.
     */
    public ResponseEntity<String> fetchCommits(String repoOwner, String repoName, String token, String branch) {
        String url = String.format("https://api.github.com/repos/%s/%s/commits?sha=%s", repoOwner, repoName, branch);
        HttpHeaders headers = buildHeaders(token, "application/vnd.github+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    /**
     * Fetch commit diff for a specific repo name and commit SHA.
     */
    public ResponseEntity<String> fetchCommitDiff(String repoOwner, String repoName, String sha, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/commits/%s", repoOwner, repoName, sha);
        HttpHeaders headers = buildHeaders(token, "application/vnd.github.v3.diff");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    private HttpHeaders buildHeaders(String token, String accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", accept);
        return headers;
    }

    public ConfigsEntity getLatestConfig() {
        return configsService.getLatestConfig();
    }
    public List<GithubRepoEntity> getAllRepos() {
        return githubRepoService.findAll();
    }
}