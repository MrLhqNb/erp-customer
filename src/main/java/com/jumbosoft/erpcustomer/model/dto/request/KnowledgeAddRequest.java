package com.jumbosoft.erpcustomer.model.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class KnowledgeAddRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private String category;
    private String tags;
    private String status = "DRAFT";
    private String source = "MANUAL";

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
}
