package com.fullmall.favorite.api;

import com.fullmall.favorite.app.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 商品收藏：需要会员登录。 */
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favorites;

    public FavoriteController(FavoriteService favorites) {
        this.favorites = favorites;
    }

    private UUID member(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest("MEMBER_REQUIRED", "需要会员登录");
        }
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public List<FavoriteService.Favorite> list(@RequestParam(defaultValue = "20") int limit,
                                               Authentication authentication) {
        return favorites.list(member(authentication), limit);
    }

    @PostMapping("/{skuId}")
    public Map<String, Boolean> add(@PathVariable UUID skuId, Authentication authentication) {
        return Map.of("added", favorites.add(member(authentication), skuId));
    }

    @DeleteMapping("/{skuId}")
    public Map<String, Boolean> remove(@PathVariable UUID skuId, Authentication authentication) {
        return Map.of("removed", favorites.remove(member(authentication), skuId));
    }
}
