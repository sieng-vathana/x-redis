package com.vyntra.redis.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Imperative Redis helper for services that prefer explicit get/put/evict
 * over Spring {@code @Cacheable} annotations.
 *
 * <p>Keys are stored as plain strings; values as JSON via the shared RedisTemplate.
 */
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String keyPrefix;
    private final Duration defaultTtl;

    public RedisCacheService(
            RedisTemplate<String, Object> redisTemplate,
            String keyPrefix,
            Duration defaultTtl) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "vyntra" : keyPrefix;
        this.defaultTtl = defaultTtl == null ? Duration.ofMinutes(10) : defaultTtl;
    }

    public String key(String cacheName, Object id) {
        return keyPrefix + ":" + cacheName + ":" + id;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String cacheName, Object id, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key(cacheName, id));
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        // Jackson may deserialize as LinkedHashMap when type info is missing
        return Optional.empty();
    }

    public void put(String cacheName, Object id, Object value) {
        put(cacheName, id, value, defaultTtl);
    }

    public void put(String cacheName, Object id, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        Duration effective = ttl != null ? ttl : defaultTtl;
        redisTemplate.opsForValue().set(key(cacheName, id), value, effective.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void evict(String cacheName, Object id) {
        redisTemplate.delete(key(cacheName, id));
    }

    /**
     * Cache-aside helper: return cached value, or load, store, and return.
     */
    public <T> T getOrLoad(String cacheName, Object id, Class<T> type, Supplier<T> loader) {
        return getOrLoad(cacheName, id, type, loader, defaultTtl);
    }

    public <T> T getOrLoad(
            String cacheName,
            Object id,
            Class<T> type,
            Supplier<T> loader,
            @Nullable Duration ttl) {
        Optional<T> cached = get(cacheName, id, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T loaded = loader.get();
        if (loaded != null) {
            put(cacheName, id, loaded, ttl);
        }
        return loaded;
    }
}
