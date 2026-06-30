package com.jumbosoft.erpcustomer.model.dto.response;

import com.jumbosoft.erpcustomer.model.entity.KnowledgeEntry;

import java.time.LocalDateTime;

public class KnowledgeResponse {

    private Long id;
    private String title;
    private String content;
    private String category;
    private String tags;
    private String status;
    private String source;
    private Integer chunkCount;
    private String embeddingModel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KnowledgeResponse from(KnowledgeEntry entry, boolean includeContent) {
        KnowledgeResponse r = new KnowledgeResponse();
        r.setId(entry.getId());
        r.setTitle(entry.getTitle());
        if (includeContent) r.setContent(entry.getContent());
        if (entry.getCategory() != null) r.setCategory(entry.getCategory().name());
        r.setTags(entry.getTags());
        if (entry.getStatus() != null) r.setStatus(entry.getStatus().name());
        r.setSource(entry.getSource());
        r.setChunkCount(entry.getChunkCount());
        r.setEmbeddingModel(entry.getEmbeddingModel());
        r.setCreatedAt(entry.getCreatedAt());
        r.setUpdatedAt(entry.getUpdatedAt());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
