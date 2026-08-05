package com.x.redis.config;

import com.x.redis.cache.CacheNames;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Tunable Redis cache settings for X services.
 *
 * <pre>
 * x:
 *   redis:
 *     enabled: true
 *     key-prefix: x
 *     default-ttl: 10m
 *     caches:
 *       product-by-id: 15m
 *       products: 5m
 * </pre>
 */
@ConfigurationProperties(prefix = "x.redis")
public class XRedisProperties {

    /**
     * Master switch. When false, Spring Cache falls back to no-op (no Redis required).
     */
    private boolean enabled = true;

    /**
     * Prefix applied to every cache key, e.g. {@code x:product-by-id::42}.
     */
    private String keyPrefix = "x";

    /**
     * Default TTL for cache regions that are not listed under {@link #caches}.
     */
    private Duration defaultTtl = Duration.ofMinutes(10);

    /** Emit structured HIT/MISS/EVICT events without logging cache keys. */
    private boolean logEvents = true;

    /**
     * Optional per-cache TTLs keyed by {@link com.x.redis.cache.CacheNames} values.
     */
    private Map<String, Duration> caches = recommendedCacheTtls();

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

    public boolean isLogEvents() {
        return logEvents;
    }

    public void setLogEvents(boolean logEvents) {
        this.logEvents = logEvents;
    }

    public Map<String, Duration> getCaches() {
        return caches;
    }

    public void setCaches(Map<String, Duration> caches) {
        Map<String, Duration> merged = recommendedCacheTtls();
        if (caches != null) {
            merged.putAll(caches);
        }
        this.caches = merged;
    }

    private static Map<String, Duration> recommendedCacheTtls() {
        Map<String, Duration> defaults = new HashMap<>();
        defaults.put(CacheNames.BUSINESSES_BY_OWNER, Duration.ofMinutes(30));
        defaults.put(CacheNames.STORES_BY_BUSINESS, Duration.ofMinutes(15));
        defaults.put(CacheNames.STORAGE_FILE_BY_RELATIVE_PATH, Duration.ofMinutes(15));
        return defaults;
    }
}
