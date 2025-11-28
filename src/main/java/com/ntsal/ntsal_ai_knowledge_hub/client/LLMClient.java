package com.ntsal.ntsal_ai_knowledge_hub.client;

import com.ntsal.ntsal_ai_knowledge_hub.entity.ConfigsEntity;
import com.ntsal.ntsal_ai_knowledge_hub.service.ConfigsService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class LLMClient {
    private final ConfigsService configsService;
    private final RestTemplate restTemplate = new RestTemplate();

    public LLMClient(ConfigsService configsService) {
        this.configsService = configsService;
    }

    public String summarizeCommit(String message, String diff) {
        ConfigsEntity config = configsService.getLatestConfig();
        String apiUrl = config.getLlmSummarizerUrl();
        String apiKey = config.getLlmApiKey();
        Map<String, Object> body = getSummaryRequestBody(message, diff, config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
            var choices = (java.util.List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                return msg.get("content").toString().trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Summary generation failed.";
    }

    public String generateFeedback(String message, String diff) {
        ConfigsEntity config = configsService.getLatestConfig();
        String apiUrl = config.getLlmSummarizerUrl();
        String apiKey = config.getLlmApiKey();
        Map<String, Object> body = getFeedbackRequestBody(message, diff, config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
            var choices = (java.util.List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                return msg.get("content").toString().trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Feedback generation failed.";
    }


    private static Map<String, Object> getSummaryRequestBody(String message, String diff, ConfigsEntity config) {
        String model = config.getLlmModel();
        String prompt = """
            You are an AI assistant that summarizes code changes.
            Given a commit message and the diff of the code,
            explain in concise and human-readable English (max 100 words)
            what this change does and why it might have been made.

            Commit Message:
            %s

            Code Diff:
            %s
        """.formatted(message, diff.length() > 2000 ? diff.substring(0, 2000) + "..." : diff);

        return Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "user", "content", prompt)
                },
                "temperature", 0.3,
                "max_tokens", 200
        );
    }

    private static Map<String, Object> getFeedbackRequestBody(String message, String diff, ConfigsEntity config) {
        String model = config.getLlmModel();
        String prompt = """
            You are an expert code reviewer. Analyze this commit and provide structured code quality feedback.
            
            **Code Quality Feedback:**
            - **Conventions & Style:** [Comment on code conventions, naming, and style adherence]
            - **Best Practices:** [Note if best practices are followed or violated]
            - **Potential Issues:** [Identify any concerns, anti-patterns, or technical debt]
            - **Suggestions:** [Recommend specific improvements if any]
            
            Keep it concise and actionable (max 150 words).

            Commit Message:
            %s

            Code Diff:
            %s
        """.formatted(message, diff.length() > 2000 ? diff.substring(0, 2000) + "..." : diff);

        return Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are an expert code reviewer with deep knowledge of software engineering best practices and code quality standards."),
                        Map.of("role", "user", "content", prompt)
                },
                "temperature", 0.3,
                "max_tokens", 250
        );
    }
}
