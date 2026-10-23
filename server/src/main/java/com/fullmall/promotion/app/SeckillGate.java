package com.fullmall.promotion.app;

import com.fullmall.platform.error.ApiException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀入口闸门（C11 骨架）：**令牌桶限流 + 有界并发**。
 *
 * <p>为什么自研而不引依赖：本机 Maven 无法访问中央仓库新增依赖（离线），
 * 令牌桶是十几行的确定性算法，测试可完全覆盖；等能联网再评估 Bucket4j。
 *
 * <p>语义：
 * <ul>
 *   <li>令牌不足 → `429 SECKILL_RATE_LIMITED`（快速失败，不排队）；</li>
 *   <li>并发槽满 → `503 SECKILL_BUSY`（让客户端稍后重试，避免雪崩）。</li>
 * </ul>
 */
public class SeckillGate {

    private final int capacity;
    private final double refillPerSecond;
    private final int maxConcurrent;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Integer> inFlight = new ConcurrentHashMap<>();

    public SeckillGate(int capacity, double refillPerSecond, int maxConcurrent) {
        if (capacity <= 0 || refillPerSecond <= 0 || maxConcurrent <= 0) {
            throw new IllegalArgumentException("限流参数必须为正数");
        }
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.maxConcurrent = maxConcurrent;
    }

    /** 取令牌；失败直接抛 429，不静默放过。 */
    public void acquire(String key) {
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("SECKILL_KEY_REQUIRED", "秒杀活动编码不能为空");
        }
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity, refillPerSecond));
        if (!bucket.tryTake()) {
            throw ApiException.tooManyRequests("SECKILL_RATE_LIMITED",
                    "秒杀请求过于频繁，请稍后重试");
        }
    }

    /** 占并发槽；满则 503。必须与 {@link #release(String)} 成对使用。 */
    public void enter(String key) {
        inFlight.merge(key, 1, Integer::sum);
        int current = inFlight.get(key);
        if (current > maxConcurrent) {
            inFlight.merge(key, -1, Integer::sum);
            throw ApiException.serviceUnavailable("SECKILL_BUSY",
                    "秒杀排队已满（并发 " + maxConcurrent + "），请稍后重试");
        }
    }

    public void release(String key) {
        inFlight.computeIfPresent(key, (ignored, value) -> value <= 1 ? null : value - 1);
    }

    public int inFlight(String key) {
        return inFlight.getOrDefault(key, 0);
    }

    private static final class Bucket {
        private final int capacity;
        private final double refillPerSecond;
        private double tokens;
        private long lastRefillNanos;

        Bucket(int capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryTake() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastRefillNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
