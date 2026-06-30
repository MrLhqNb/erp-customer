package com.jumbosoft.erpcustomer.model.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class KnowledgeBatchImportRequest {

    @NotEmpty(message = "导入数据不能为空")
    @Valid
    private List<KnowledgeImportItem> items;

    public List<KnowledgeImportItem> getItems() { return items; }
    public void setItems(List<KnowledgeImportItem> items) { this.items = items; }

    public static class KnowledgeImportItem {
        @javax.validation.constraints.NotBlank(message = "标题不能为空")
        private String title;

        @javax.validation.constraints.NotBlank(message = "内容不能为空")
        private String content;

        private String category;
        private String tags;
        private String status = "DRAFT";

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
    }
}
