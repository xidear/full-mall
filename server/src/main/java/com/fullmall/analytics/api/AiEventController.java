package com.fullmall.analytics.api;

import com.fullmall.analytics.app.AiEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 行为事件上报与统计（K7）。上报是公开接口，统计仅后台。 */
@RestController
public class AiEventController {

    private final AiEventService events;

    public AiEventController(AiEventService events) {
        this.events = events;
    }

    public record EventRequest(String eventType, UUID memberId, String anonymousId,
                               UUID skuId, String keyword, Map<String, Object> payload) {
    }

    /** 前端埋点上报（匿名可用）。 */
    @PostMapping("/api/v1/events")
    public Map<String, Object> record(@RequestBody EventRequest body) {
        events.record(body.eventType(), body.memberId(), body.anonymousId(),
                body.skuId(), body.keyword(), body.payload());
        return Map.of("accepted", true);
    }

    @GetMapping("/admin/v1/ai/events/top-keywords")
    public List<Map<String, Object>> topKeywords(@RequestParam(defaultValue = "10") int limit) {
        return events.topKeywords(limit);
    }

    @GetMapping("/admin/v1/ai/events/top-added-skus")
    public List<Map<String, Object>> topAddedSkus(@RequestParam(defaultValue = "10") int limit) {
        return events.topAddedSkus(limit);
    }
}
