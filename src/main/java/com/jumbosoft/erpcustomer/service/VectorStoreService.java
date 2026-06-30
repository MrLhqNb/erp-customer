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
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final RestTemplate restTemplate;
    private final EmbeddingProperties embeddingProps;
    private final ObjectMapper objectMapper;

    public VectorStoreService(RestTemplate restTemplate, EmbeddingProperties embeddingProps) {
        this.restTemplate = restTemplate;
        this.embeddingProps = embeddingProps;
        this.objectMapper = new ObjectMapper();
    }

    public void ensureCollection() {
        // Milvus Lite auto-creates on first use, no init needed
        log.info("Vector store ready (Milvus Lite via Python)");
    }

    public void addDocuments(List<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) return;

        String url = embeddingProps.getBaseUrl() + "/api/vectors/add";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> docList = new ArrayList<>();
        for (VectorDocument doc : documents) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", doc.getId());
            m.put("document", doc.getDocument());
            List<Float> emb = new ArrayList<>();
            if (doc.getEmbedding() != null) {
                for (float v : doc.getEmbedding()) emb.add(v);
            }
            m.put("embedding", emb);
            m.put("knowledge_id", doc.getKnowledgeId());
            m.put("title", doc.getTitle());
            m.put("chunk_index", doc.getChunkIndex());
            m.put("chunk_count", doc.getChunkCount());
            docList.add(m);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("documents", docList);

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);
            log.info("Added {} vectors via Python", documents.size());
        } catch (Exception e) {
            log.error("Failed to add vectors", e);
            throw new BusinessException(5003, "Vector insert error: " + e.getMessage());
        }
    }

    public List<VectorQueryResult> query(float[] queryEmbedding, int topK) {
        // Queries go directly to Milvus Lite via Python (not used from Java side currently)
        return Collections.emptyList();
    }

    public void deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;

        String url = embeddingProps.getBaseUrl() + "/api/vectors/delete";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);
            log.info("Deleted {} vectors via Python", ids.size());
        } catch (Exception e) {
            log.error("Failed to delete vectors", e);
            throw new BusinessException(5005, "Vector delete error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<VectorQueryResult> listAll(int limit) {
        String url = embeddingProps.getBaseUrl() + "/api/vectors/list?limit=" + limit;
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            List<VectorQueryResult> results = new ArrayList<>();
            for (JsonNode node : root) {
                VectorQueryResult r = new VectorQueryResult();
                r.setId(node.has("id") ? node.get("id").asText() : "");
                r.setDocument(node.has("document") ? node.get("document").asText() : "");
                r.setKnowledgeId(node.has("knowledge_id") ? node.get("knowledge_id").asLong() : 0);
                r.setTitle(node.has("title") ? node.get("title").asText() : "");
                r.setChunkIndex(node.has("chunk_index") ? node.get("chunk_index").asInt() : 0);
                r.setChunkCount(node.has("chunk_count") ? node.get("chunk_count").asInt() : 0);
                results.add(r);
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to list vectors", e);
            return Collections.emptyList();
        }
    }
}
