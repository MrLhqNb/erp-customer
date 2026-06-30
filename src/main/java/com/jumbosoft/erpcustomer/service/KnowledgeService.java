package com.jumbosoft.erpcustomer.service;

import com.jumbosoft.erpcustomer.config.EmbeddingProperties;
import com.jumbosoft.erpcustomer.exception.BusinessException;
import com.jumbosoft.erpcustomer.model.entity.KnowledgeEntry;
import com.jumbosoft.erpcustomer.model.enums.KnowledgeStatus;
import com.jumbosoft.erpcustomer.repository.KnowledgeEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeEntryRepository repository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final TextChunkService textChunkService;
    private final EmbeddingProperties embeddingProperties;

    public KnowledgeService(KnowledgeEntryRepository repository,
                            EmbeddingService embeddingService,
                            VectorStoreService vectorStoreService,
                            TextChunkService textChunkService,
                            EmbeddingProperties embeddingProperties) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.textChunkService = textChunkService;
        this.embeddingProperties = embeddingProperties;
    }

    public KnowledgeEntry add(String title, String content, String category, String tags, String status, String source) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(title);
        entry.setContent(content);
        if (category != null) {
            try {
                entry.setCategory(com.jumbosoft.erpcustomer.model.enums.KnowledgeCategory.valueOf(category));
            } catch (IllegalArgumentException e) {
                entry.setCategory(com.jumbosoft.erpcustomer.model.enums.KnowledgeCategory.GENERAL);
            }
        }
        entry.setTags(tags);
        entry.setStatus(KnowledgeStatus.valueOf(status != null ? status : "DRAFT"));
        entry.setSource(source != null ? source : "MANUAL");

        KnowledgeEntry saved = repository.save(entry);

        if (saved.getStatus() == KnowledgeStatus.ACTIVE) {
            embedAndStore(saved);
            saved = repository.save(saved);
        }

        log.info("Added knowledge entry: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    public KnowledgeEntry update(Long id, String title, String content, String category, String tags, String status) {
        KnowledgeEntry entry = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Knowledge entry not found: " + id));

        boolean contentChanged = false;
        if (title != null) entry.setTitle(title);
        if (content != null && !content.equals(entry.getContent())) {
            entry.setContent(content);
            contentChanged = true;
        }
        if (category != null) {
            try {
                entry.setCategory(com.jumbosoft.erpcustomer.model.enums.KnowledgeCategory.valueOf(category));
            } catch (IllegalArgumentException ignored) {}
        }
        if (tags != null) entry.setTags(tags);
        if (status != null) entry.setStatus(KnowledgeStatus.valueOf(status));

        KnowledgeEntry saved = repository.save(entry);

        if (contentChanged || (saved.getStatus() == KnowledgeStatus.ACTIVE && saved.getChunkCount() == 0)) {
            // Remove old vectors then re-embed
            removeVectors(id, saved.getChunkCount());
            if (saved.getStatus() == KnowledgeStatus.ACTIVE) {
                embedAndStore(saved);
                saved = repository.save(saved);
            }
        }

        log.info("Updated knowledge entry: id={}", saved.getId());
        return saved;
    }

    public void delete(Long id) {
        KnowledgeEntry entry = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Knowledge entry not found: " + id));

        removeVectors(id, entry.getChunkCount());
        repository.delete(entry);
        log.info("Deleted knowledge entry: id={}", id);
    }

    public KnowledgeEntry getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Knowledge entry not found: " + id));
    }

    public Page<KnowledgeEntry> query(String keyword, String category, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        if (keyword != null && !keyword.isEmpty()) {
            return repository.searchActive(keyword, pageable);
        }

        if (status != null && category != null) {
            KnowledgeStatus s = KnowledgeStatus.valueOf(status);
            com.jumbosoft.erpcustomer.model.enums.KnowledgeCategory c =
                    com.jumbosoft.erpcustomer.model.enums.KnowledgeCategory.valueOf(category);
            return repository.findByCategoryAndStatus(c, s, pageable);
        }

        if (status != null) {
            return repository.findByStatus(KnowledgeStatus.valueOf(status), pageable);
        }

        return repository.findAll(pageable);
    }

    public KnowledgeEntry reEmbed(Long id) {
        KnowledgeEntry entry = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Knowledge entry not found: " + id));

        if (entry.getStatus() == KnowledgeStatus.ACTIVE) {
            removeVectors(id, entry.getChunkCount());
            embedAndStore(entry);
            repository.save(entry);
        }
        return entry;
    }

    public Map<String, Object> reEmbedAll(String category, String status) {
        List<KnowledgeEntry> entries;
        if (status != null) {
            entries = repository.findByStatus(KnowledgeStatus.valueOf(status));
        } else {
            entries = repository.findByStatus(KnowledgeStatus.ACTIVE);
        }

        if (category != null) {
            com.jumbosoft.erpcustomer.model.enums.KnowledgeCategory c =
                    com.jumbosoft.erpcustomer.model.enums.KnowledgeCategory.valueOf(category);
            entries = entries.stream().filter(e -> e.getCategory() == c).collect(Collectors.toList());
        }

        int success = 0;
        List<Map<String, Object>> failures = new ArrayList<>();

        for (KnowledgeEntry entry : entries) {
            try {
                removeVectors(entry.getId(), entry.getChunkCount());
                embedAndStore(entry);
                repository.save(entry);
                success++;
            } catch (Exception e) {
                log.error("Failed to re-embed entry: id={}", entry.getId(), e);
                Map<String, Object> fail = new HashMap<>();
                fail.put("id", entry.getId());
                fail.put("title", entry.getTitle());
                fail.put("error", e.getMessage());
                failures.add(fail);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", entries.size());
        result.put("successCount", success);
        result.put("failCount", failures.size());
        result.put("failures", failures);
        return result;
    }

    public Map<String, Object> batchImport(List<Map<String, String>> items) {
        int success = 0;
        List<Map<String, Object>> failures = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, String> item = items.get(i);
            try {
                add(
                    item.get("title"),
                    item.get("content"),
                    item.get("category"),
                    item.get("tags"),
                    item.getOrDefault("status", "ACTIVE"),
                    "BATCH_IMPORT"
                );
                success++;
            } catch (Exception e) {
                log.error("Failed to import item #{}", i, e);
                Map<String, Object> fail = new HashMap<>();
                fail.put("index", i);
                fail.put("title", item.get("title"));
                fail.put("error", e.getMessage());
                failures.add(fail);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", items.size());
        result.put("successCount", success);
        result.put("failCount", failures.size());
        result.put("failures", failures);
        return result;
    }

    public Map<String, Object> batchDelete(List<Long> ids) {
        int success = 0;
        List<Map<String, Object>> failures = new ArrayList<>();

        for (Long id : ids) {
            try {
                delete(id);
                success++;
            } catch (Exception e) {
                log.error("Failed to delete entry: id={}", id, e);
                Map<String, Object> fail = new HashMap<>();
                fail.put("id", id);
                fail.put("error", e.getMessage());
                failures.add(fail);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("successCount", success);
        result.put("failCount", failures.size());
        result.put("failures", failures);
        return result;
    }

    private void embedAndStore(KnowledgeEntry entry) {
        List<String> chunks = textChunkService.split(entry.getContent());
        if (chunks.isEmpty()) return;

        List<float[]> embeddings = embeddingService.embedBatch(chunks);

        List<VectorDocument> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            VectorDocument doc = new VectorDocument();
            doc.setId("kb:" + entry.getId() + ":chunk:" + i);
            doc.setDocument(chunks.get(i));
            doc.setEmbedding(embeddings.get(i));
            doc.setKnowledgeId(entry.getId());
            doc.setTitle(entry.getTitle());
            doc.setChunkIndex(i);
            doc.setChunkCount(chunks.size());
            documents.add(doc);
        }

        vectorStoreService.addDocuments(documents);
        entry.setChunkCount(chunks.size());
        entry.setEmbeddingModel(embeddingProperties.getModel());
    }

    private void removeVectors(Long knowledgeId, int chunkCount) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            ids.add("kb:" + knowledgeId + ":chunk:" + i);
        }
        if (!ids.isEmpty()) {
            vectorStoreService.deleteByIds(ids);
        }
    }
}
