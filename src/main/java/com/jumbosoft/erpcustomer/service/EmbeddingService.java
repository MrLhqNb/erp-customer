package com.jumbosoft.erpcustomer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbosoft.erpcustomer.config.EmbeddingProperties;
import com.jumbosoft.erpcustomer.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final RestTemplate restTemplate;
    private final EmbeddingProperties props;
    private final ObjectMapper objectMapper;

    public EmbeddingService(RestTemplate restTemplate, EmbeddingProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.objectMapper = new ObjectMapper();
    }

    public float[] embed(String text) {
        List<float[]> results = embedBatch(Collections.singletonList(text));
        return results.get(0);
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        String url = props.getBaseUrl() + "/api/embed";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("texts", texts);

        int attempts = 0;
        while (true) {
            try {
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingsNode = root.get("embeddings");
                List<float[]> embeddings = new ArrayList<>();

                for (JsonNode item : embeddingsNode) {
                    float[] embedding = new float[item.size()];
                    for (int i = 0; i < item.size(); i++) {
                        embedding[i] = (float) item.get(i).asDouble();
                    }
                    embeddings.add(embedding);
                }
                return embeddings;

            } catch (Exception e) {
                attempts++;
                if (attempts > 3) {
                    log.error("Embedding API call failed after {} attempts", attempts, e);
                    throw new BusinessException(5001, "Embedding service error: " + e.getMessage(), e);
                }
                log.warn("Embedding attempt {}/3 failed, retrying...", attempts);
                try {
                    Thread.sleep(1000L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(5001, "Embedding interrupted");
                }
            }
        }
    }
}
