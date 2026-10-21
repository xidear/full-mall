package com.fullmall.ai.app;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 内存知识库（演示模式用）。
 *
 * <p>检索是**关键词/字粒度匹配**，不是语义检索——演示时够用，但要清楚它答不了
 * "同义词/换句话问"的问题。生产必须换 pgvector 实现，这一点写在文档里，不假装等价。
 */
@Component
public class InMemoryKnowledgeStore implements KnowledgeStore {

    private final Map<String, List<KnowledgeChunk>> bySource = new LinkedHashMap<>();

    @Override
    public synchronized void replaceSource(String sourceType, String sourceId, List<KnowledgeChunk> chunks) {
        bySource.put(key(sourceType, sourceId), List.copyOf(chunks));
    }

    @Override
    public synchronized List<KnowledgeChunk> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        for (String raw : query.toLowerCase(Locale.ROOT).split("[\\s,，。？?！!；;：:]+")) {
            if (raw.length() >= 2) {
                terms.add(raw);
            }
        }
        if (terms.isEmpty()) {
            return List.of();
        }

        List<Scored> scored = new ArrayList<>();
        for (List<KnowledgeChunk> chunks : bySource.values()) {
            for (KnowledgeChunk chunk : chunks) {
                String haystack = (chunk.title() + " " + chunk.content()).toLowerCase(Locale.ROOT);
                int hits = 0;
                for (String term : terms) {
                    if (haystack.contains(term)) {
                        hits++;
                    }
                }
                if (hits > 0) {
                    scored.add(new Scored(chunk, hits));
                }
            }
        }
        scored.sort((a, b) -> Integer.compare(b.hits, a.hits));
        return scored.stream().limit(topK).map(Scored::chunk).toList();
    }

    @Override
    public synchronized int size() {
        return bySource.values().stream().mapToInt(List::size).sum();
    }

    private String key(String sourceType, String sourceId) {
        return sourceType + ":" + sourceId;
    }

    private record Scored(KnowledgeChunk chunk, int hits) {
    }
}
