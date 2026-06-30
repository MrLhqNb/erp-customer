package com.jumbosoft.erpcustomer.model.dto.request;

public class ReEmbedRequest {

    private String category;
    private String embeddingModel;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
}
