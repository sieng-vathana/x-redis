package com.x.redis.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CacheEventLoggingCacheManagerTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void reportsMissThenHitWithoutReceivingTheCacheKey() {
        List<String> events = new ArrayList<>();
        var manager = new CacheEventLoggingCacheManager(
                new ConcurrentMapCacheManager(CacheNames.PRODUCTS),
                "x-product-service",
                (service, cache, result, correlationId) ->
                        events.add(service + "|" + cache + "|" + result + "|" + correlationId));
        Cache cache = manager.getCache(CacheNames.PRODUCTS);
        assertNotNull(cache);
        MDC.put("correlationId", "request-123");

        assertNull(cache.get("private-cache-key"));
        cache.put("private-cache-key", "product-page");
        assertEquals("product-page", cache.get("private-cache-key", String.class));

        assertEquals(List.of(
                "x-product-service|products|MISS|request-123",
                "x-product-service|products|HIT|request-123"), events);
    }
}
