package com.jumbosoft.erpcustomer.controller;

import com.jumbosoft.erpcustomer.exception.ApiResult;
import com.jumbosoft.erpcustomer.model.dto.request.KnowledgeAddRequest;
import com.jumbosoft.erpcustomer.model.dto.request.KnowledgeBatchImportRequest;
import com.jumbosoft.erpcustomer.model.dto.request.KnowledgeUpdateRequest;
import com.jumbosoft.erpcustomer.model.dto.request.ReEmbedRequest;
import com.jumbosoft.erpcustomer.model.dto.response.KnowledgeResponse;
import com.jumbosoft.erpcustomer.model.entity.KnowledgeEntry;
import com.jumbosoft.erpcustomer.service.KnowledgeService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final com.jumbosoft.erpcustomer.service.VectorStoreService vectorStoreService;

    public KnowledgeController(KnowledgeService knowledgeService,
                               com.jumbosoft.erpcustomer.service.VectorStoreService vectorStoreService) {
        this.knowledgeService = knowledgeService;
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping
    public ApiResult<KnowledgeResponse> add(@Valid @RequestBody KnowledgeAddRequest request) {
        KnowledgeEntry entry = knowledgeService.add(
                request.getTitle(), request.getContent(), request.getCategory(),
                request.getTags(), request.getStatus(), request.getSource());
        return ApiResult.ok(KnowledgeResponse.from(entry, false));
    }

    @PutMapping("/{id}")
    public ApiResult<KnowledgeResponse> update(@PathVariable Long id,
                                                @RequestBody KnowledgeUpdateRequest request) {
        KnowledgeEntry entry = knowledgeService.update(id,
                request.getTitle(), request.getContent(), request.getCategory(),
                request.getTags(), request.getStatus());
        return ApiResult.ok(KnowledgeResponse.from(entry, false));
    }

    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ApiResult.ok();
    }

    @GetMapping("/{id}")
    public ApiResult<KnowledgeResponse> get(@PathVariable Long id) {
        KnowledgeEntry entry = knowledgeService.getById(id);
        return ApiResult.ok(KnowledgeResponse.from(entry, false));
    }

    @GetMapping("/{id}/detail")
    public ApiResult<KnowledgeResponse> getDetail(@PathVariable Long id) {
        KnowledgeEntry entry = knowledgeService.getById(id);
        return ApiResult.ok(KnowledgeResponse.from(entry, true));
    }

    @GetMapping
    public ApiResult<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        Page<KnowledgeEntry> result = knowledgeService.query(keyword, category, status, page, size);
        List<KnowledgeResponse> items = result.getContent().stream()
                .map(e -> KnowledgeResponse.from(e, false))
                .collect(Collectors.toList());

        Map<String, Object> pageResult = new java.util.LinkedHashMap<>();
        pageResult.put("items", items);
        pageResult.put("totalElements", result.getTotalElements());
        pageResult.put("totalPages", result.getTotalPages());
        pageResult.put("page", page);
        pageResult.put("size", size);
        return ApiResult.ok(pageResult);
    }

    @PostMapping("/batch-import")
    public ApiResult<Map<String, Object>> batchImport(@Valid @RequestBody KnowledgeBatchImportRequest request) {
        List<Map<String, String>> items = request.getItems().stream().map(item -> {
            Map<String, String> m = new java.util.LinkedHashMap<>();
            m.put("title", item.getTitle());
            m.put("content", item.getContent());
            if (item.getCategory() != null) m.put("category", item.getCategory());
            if (item.getTags() != null) m.put("tags", item.getTags());
            if (item.getStatus() != null) m.put("status", item.getStatus());
            return m;
        }).collect(Collectors.toList());
        return ApiResult.ok(knowledgeService.batchImport(items));
    }

    @PostMapping("/batch-delete")
    public ApiResult<Map<String, Object>> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResult.fail(400, "ids不能为空");
        }
        return ApiResult.ok(knowledgeService.batchDelete(ids));
    }

    @PostMapping("/{id}/re-embed")
    public ApiResult<KnowledgeResponse> reEmbed(@PathVariable Long id) {
        KnowledgeEntry entry = knowledgeService.reEmbed(id);
        return ApiResult.ok(KnowledgeResponse.from(entry, false));
    }

    @PostMapping("/re-embed-all")
    public ApiResult<Map<String, Object>> reEmbedAll(@RequestBody(required = false) ReEmbedRequest request) {
        String category = request != null ? request.getCategory() : null;
        Map<String, Object> result = knowledgeService.reEmbedAll(category, null);
        return ApiResult.ok(result);
    }

    @GetMapping("/vectordb-data")
    public ApiResult<List<com.jumbosoft.erpcustomer.service.VectorQueryResult>> vectordbData(
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResult.ok(vectorStoreService.listAll(limit));
    }
}
