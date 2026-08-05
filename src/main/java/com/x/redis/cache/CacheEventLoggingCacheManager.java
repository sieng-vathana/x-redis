package com.x.redis.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Logs cache reads in a consistent format without exposing cache keys. */
public final class CacheEventLoggingCacheManager implements CacheManager {

    private static final Logger EVENT_LOG = LoggerFactory.getLogger("com.x.redis.cache.CacheEvents");
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final CacheManager delegate;
    private final String serviceName;
    private final CacheEventSink eventSink;
    private final Map<String, Cache> cacheWrappers = new ConcurrentHashMap<>();

    public CacheEventLoggingCacheManager(CacheManager delegate, String serviceName) {
        this(delegate, serviceName, (service, cache, result, correlationId) -> EVENT_LOG.info(
                "★★★ CACHE-EVENT ★★★ service={} cache={} result={} correlationId={}",
                service, cache, result, correlationId));
    }

    CacheEventLoggingCacheManager(CacheManager delegate, String serviceName, CacheEventSink eventSink) {
        this.delegate = delegate;
        this.serviceName = serviceName == null || serviceName.isBlank() ? "unknown" : serviceName;
        this.eventSink = eventSink;
    }

    @Override
    @Nullable
    public Cache getCache(String name) {
        return cacheWrappers.computeIfAbsent(name, cacheName -> {
            Cache cache = delegate.getCache(cacheName);
            return cache == null ? null : new CacheEventLoggingCache(cache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    private void log(String cacheName, String result) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        eventSink.log(serviceName, cacheName, result,
                correlationId == null || correlationId.isBlank() ? "-" : correlationId);
    }

    @FunctionalInterface
    interface CacheEventSink {
        void log(String service, String cache, String result, String correlationId);
    }

    private final class CacheEventLoggingCache implements Cache {

        private final Cache cache;

        private CacheEventLoggingCache(Cache cache) {
            this.cache = cache;
        }

        @Override
        public String getName() {
            return cache.getName();
        }

        @Override
        public Object getNativeCache() {
            return cache.getNativeCache();
        }

        @Override
        @Nullable
        public ValueWrapper get(Object key) {
            ValueWrapper value = cache.get(key);
            log(getName(), value == null ? "MISS" : "HIT");
            return value;
        }

        @Override
        @Nullable
        public <T> T get(Object key, @Nullable Class<T> type) {
            T value = cache.get(key, type);
            log(getName(), value == null ? "MISS" : "HIT");
            return value;
        }

        @Override
        @Nullable
        public <T> T get(Object key, Callable<T> valueLoader) {
            AtomicBoolean loaded = new AtomicBoolean(false);
            T value = cache.get(key, () -> {
                loaded.set(true);
                return valueLoader.call();
            });
            log(getName(), loaded.get() ? "MISS" : "HIT");
            return value;
        }

        @Override
        @Nullable
        public CompletableFuture<?> retrieve(Object key) {
            CompletableFuture<?> value = cache.retrieve(key);
            if (value == null) {
                log(getName(), "MISS");
                return null;
            }
            return value.whenComplete((result, error) -> {
                if (error == null) {
                    log(getName(), result == null ? "MISS" : "HIT");
                }
            });
        }

        @Override
        public <T> CompletableFuture<T> retrieve(
                Object key,
                Supplier<CompletableFuture<T>> valueLoader) {
            AtomicBoolean loaded = new AtomicBoolean(false);
            CompletableFuture<T> value = cache.retrieve(key, () -> {
                loaded.set(true);
                return valueLoader.get();
            });
            return value.whenComplete((result, error) -> {
                if (error == null) {
                    log(getName(), loaded.get() ? "MISS" : "HIT");
                }
            });
        }

        @Override
        public void put(Object key, @Nullable Object value) {
            cache.put(key, value);
        }

        @Override
        @Nullable
        public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
            return cache.putIfAbsent(key, value);
        }

        @Override
        public void evict(Object key) {
            cache.evict(key);
            log(getName(), "EVICT");
        }

        @Override
        public boolean evictIfPresent(Object key) {
            boolean evicted = cache.evictIfPresent(key);
            if (evicted) {
                log(getName(), "EVICT");
            }
            return evicted;
        }

        @Override
        public void clear() {
            cache.clear();
            log(getName(), "EVICT_ALL");
        }

        @Override
        public boolean invalidate() {
            boolean invalidated = cache.invalidate();
            if (invalidated) {
                log(getName(), "EVICT_ALL");
            }
            return invalidated;
        }
    }
}
