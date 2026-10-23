package com.fullmall.promotion.config;

import com.fullmall.promotion.app.SeckillGate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 秒杀闸门装配：容量/补充速率/并发上限均可配。 */
@Configuration
public class SeckillConfig {

    @Bean
    public SeckillGate seckillGate(
            @Value("${app.seckill.capacity:200}") int capacity,
            @Value("${app.seckill.refill-per-second:100}") double refillPerSecond,
            @Value("${app.seckill.max-concurrent:50}") int maxConcurrent) {
        return new SeckillGate(capacity, refillPerSecond, maxConcurrent);
    }
}
