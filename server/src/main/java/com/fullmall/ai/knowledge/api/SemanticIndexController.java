package com.fullmall.ai.knowledge.api;

import com.fullmall.ai.knowledge.SemanticIndexService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 语义检索：建索引、批量重建、查询（后台）。 */
@RestController
@RequestMapping("/admin/v1/ai/semantic")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class SemanticIndexController {

    private final SemanticIndexService index;

    public SemanticIndexController(SemanticIndexService index) {
        this.index = index;
    }

    public record IndexRequest(String kind, String sourceId, String content, String providerCode) {
    }

    @PostMapping("/index")
    public Map<String, Object> indexOne(@RequestBody IndexRequest body) {
        return Map.of("indexed", index.index(body.kind(), body.sourceId(), body.content(),
                body.providerCode()));
    }

    @PostMapping("/reindex")
    public Map<String, Object> reindex(@RequestParam String kind,
                                       @RequestParam(required = false) String providerCode) {
        return Map.of("indexed", index.reindexKind(kind, providerCode));
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q,
                                            @RequestParam(required = false) String kind,
                                            @RequestParam(defaultValue = "5") int limit,
                                            @RequestParam(required = false) String providerCode) {
        return index.search(q, kind, limit, providerCode);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("enabled", index.enabled());
    }
}
