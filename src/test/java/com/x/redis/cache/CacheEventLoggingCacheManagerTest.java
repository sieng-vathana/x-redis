package com.x.redis.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void evictsUnreadableEntryAndReportsRecoveredMiss() {
        List<String> events = new ArrayList<>();
        CacheManager delegateManager = mock(CacheManager.class);
        Cache delegateCache = mock(Cache.class);
        when(delegateManager.getCache(CacheNames.STORES_BY_BUSINESS)).thenReturn(delegateCache);
        when(delegateCache.getName()).thenReturn(CacheNames.STORES_BY_BUSINESS);
        when(delegateCache.get("2:0:100")).thenThrow(new SerializationException("missing type metadata"));
        var manager = new CacheEventLoggingCacheManager(
                delegateManager,
                "x-store-service",
                (service, cache, result, correlationId) ->
                        events.add(service + "|" + cache + "|" + result + "|" + correlationId));
        Cache cache = manager.getCache(CacheNames.STORES_BY_BUSINESS);
        assertNotNull(cache);
        MDC.put("correlationId", "request-456");

        assertNull(cache.get("2:0:100"));

        verify(delegateCache).evict("2:0:100");
        assertEquals(List.of(
                "x-store-service|stores-by-business|RECOVERED_MISS|request-456"), events);
    }
}
