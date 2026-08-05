package com.x.redis.config;

import com.x.redis.cache.CacheNames;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XRedisPropertiesTest {

    @Test
    void providesRecommendedLoginDependencyTtlsByDefault() {
        XRedisProperties properties = new XRedisProperties();

        assertTrue(properties.isLogEvents());
        assertEquals(Duration.ofMinutes(30), properties.getCaches().get(CacheNames.BUSINESSES_BY_OWNER));
        assertEquals(Duration.ofMinutes(15), properties.getCaches().get(CacheNames.STORES_BY_BUSINESS));
        assertEquals(Duration.ofMinutes(15),
                properties.getCaches().get(CacheNames.STORAGE_FILE_BY_RELATIVE_PATH));
    }

    @Test
    void configuredTtlsOverrideDefaultsWithoutDroppingOtherRecommendations() {
        XRedisProperties properties = new XRedisProperties();

        properties.setCaches(Map.of(CacheNames.STORES_BY_BUSINESS, Duration.ofMinutes(5)));

        assertEquals(Duration.ofMinutes(5), properties.getCaches().get(CacheNames.STORES_BY_BUSINESS));
        assertEquals(Duration.ofMinutes(30), properties.getCaches().get(CacheNames.BUSINESSES_BY_OWNER));
    }
}
