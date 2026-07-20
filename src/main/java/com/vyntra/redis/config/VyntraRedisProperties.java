package com.vyntra.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Tunable Redis cache settings for Vyntra services.
 *
 * <pre>
 * vyntra:
 *   redis:
 *     enabled: true
 *     key-prefix: vyntra
 *     default-ttl: 10m
 *     caches:
 *       product-by-id: 15m
 *       products: 5m
 * </pre>
 */
@ConfigurationProperties(prefix = "vyntra.redis")
public class VyntraRedisProperties {

    /**
     * Master switch. When false, Spring Cache falls back to no-op (no Redis required).
     */
    private boolean enabled = true;

    /**
     * Prefix applied to every cache key, e.g. {@code vyntra:product-by-id::42}.
     */
    private String keyPrefix = "vyntra";

    /**
     * Default TTL for cache regions that are not listed under {@link #caches}.
     */
    private Duration defaultTtl = Duration.ofMinutes(10);

    /**
     * Optional per-cache TTLs keyed by {@link com.vyntra.redis.cache.CacheNames} values.
     */
    private Map<String, Duration> caches = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Duration> getCaches() {
        return caches;
    }

    public void setCaches(Map<String, Duration> caches) {
        this.caches = caches != null ? caches : new HashMap<>();
    }
}
