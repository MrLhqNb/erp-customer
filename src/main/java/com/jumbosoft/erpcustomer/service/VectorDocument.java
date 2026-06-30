package com.jumbosoft.erpcustomer.service;

public class VectorDocument {
    private String id;
    private String document;
    private float[] embedding;
    private long knowledgeId;
    private String title;
    private int chunkIndex;
    private int chunkCount;

    public VectorDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public long getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(long knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
}
