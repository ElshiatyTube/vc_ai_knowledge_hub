package com.ntsal.ntsal_ai_knowledge_hub.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${embedding.service.url:http://embedding_service:8000}")
    private String embeddingServiceUrl;

    public float[] generateEmbedding(String text) {
        // Python service expects {"texts": ["..."]}
        Map<String, Object> body = Map.of("texts", List.of(text));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // Use /embed-single endpoint - most efficient for single text embedding
            String url = embeddingServiceUrl + "/embed-single";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("embedding")) {
                List<Double> vector = (List<Double>) responseBody.get("embedding");
                float[] floatArray = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    floatArray[i] = vector.get(i).floatValue();
                }
                return floatArray;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // fallback
        return new float[0];
    }
}
