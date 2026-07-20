package com.x.redis.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheServiceTest {

    @Test
    void keyUsesPrefixAndCacheName() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        RedisCacheService service = new RedisCacheService(template, "x", Duration.ofMinutes(5));

        assertEquals("x:product-by-id:42", service.key(CacheNames.PRODUCT_BY_ID, 42L));
    }

    @Test
    void getOrLoadReturnsCachedValueWithoutCallingLoader() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("x:product-by-id:1")).thenReturn("cached-product");

        RedisCacheService service = new RedisCacheService(template, "x", Duration.ofMinutes(5));
        AtomicInteger loads = new AtomicInteger();

        String result = service.getOrLoad(
                CacheNames.PRODUCT_BY_ID,
                1L,
                String.class,
                () -> {
                    loads.incrementAndGet();
                    return "loaded";
                });

        assertEquals("cached-product", result);
        assertEquals(0, loads.get());
        verify(ops, never()).set(any(), any(), anyLong(), any());
    }

    @Test
    void getOrLoadStoresValueOnMiss() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("x:product-by-id:2")).thenReturn(null);

        RedisCacheService service = new RedisCacheService(template, "x", Duration.ofMinutes(5));

        String result = service.getOrLoad(CacheNames.PRODUCT_BY_ID, 2L, String.class, () -> "fresh");

        assertEquals("fresh", result);
        verify(ops).set(eq("x:product-by-id:2"), eq("fresh"), eq(Duration.ofMinutes(5).toMillis()), any());
    }

    @Test
    void getReturnsEmptyWhenMissing() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("x:products:all")).thenReturn(null);

        RedisCacheService service = new RedisCacheService(template, "x", Duration.ofMinutes(5));
        Optional<String> value = service.get(CacheNames.PRODUCTS, "all", String.class);

        assertTrue(value.isEmpty());
    }
}
