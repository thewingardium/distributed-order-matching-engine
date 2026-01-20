package com.example.dome.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MarketDataCache {

    private final StringRedisTemplate redisTemplate;

    public MarketDataCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateBestBid(String symbol, BigDecimal price) {
        if (price == null) return;
        try {
            redisTemplate.opsForValue().set("market:" + symbol + ":bestBid", price.toString());
        } catch (Exception e) {
            // Fallback or log error if Redis is down, don't crash the engine
            System.err.println("Failed to update Redis cache (Best Bid): " + e.getMessage());
        }
    }

    public void updateBestAsk(String symbol, BigDecimal price) {
        if (price == null) return;
        try {
            redisTemplate.opsForValue().set("market:" + symbol + ":bestAsk", price.toString());
        } catch (Exception e) {
            System.err.println("Failed to update Redis cache (Best Ask): " + e.getMessage());
        }
    }

    public String getBestBid(String symbol) {
        try {
            return redisTemplate.opsForValue().get("market:" + symbol + ":bestBid");
        } catch (Exception e) {
            return null;
        }
    }

    public String getBestAsk(String symbol) {
        try {
            return redisTemplate.opsForValue().get("market:" + symbol + ":bestAsk");
        } catch (Exception e) {
            return null;
        }
    }
}
