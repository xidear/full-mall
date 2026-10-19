package com.fullmall.history.api;

import com.fullmall.history.app.BrowseHistoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 浏览足迹：需会员登录。 */
@RestController
@RequestMapping("/api/v1/history")
public class BrowseHistoryController {

    private final BrowseHistoryService history;

    public BrowseHistoryController(BrowseHistoryService history) {
        this.history = history;
    }

    private UUID member(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest("MEMBER_REQUIRED", "需要会员登录");
        }
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public List<BrowseHistoryService.Entry> recent(@RequestParam(defaultValue = "20") int limit,
                                                   Authentication authentication) {
        return history.recent(member(authentication), limit);
    }

    @PostMapping("/{skuId}")
    public void record(@PathVariable UUID skuId, Authentication authentication) {
        history.record(member(authentication), skuId);
    }

    @DeleteMapping
    public Map<String, Integer> clear(Authentication authentication) {
        return Map.of("cleared", history.clear(member(authentication)));
    }
}
