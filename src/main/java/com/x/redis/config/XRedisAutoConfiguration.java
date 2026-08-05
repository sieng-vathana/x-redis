package com.x.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.x.redis.cache.CacheNames;
import com.x.redis.cache.CacheEventLoggingCacheManager;
import com.x.redis.cache.RedisCacheService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configures Redis-backed Spring Cache + a small {@link RedisCacheService} helper.
 *
 * <p>Activate in a service by depending on {@code com.x:x-redis} and setting
 * {@code spring.data.redis.host}/{@code port}. Disable with {@code x.redis.enabled=false}.
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@EnableCaching
@EnableConfigurationProperties(XRedisProperties.class)
@ConditionalOnClass({RedisConnectionFactory.class, RedisCacheManager.class})
@ConditionalOnProperty(prefix = "x.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "xRedisTemplate")
    public RedisTemplate<String, Object> xRedisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = jsonSerializer();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            XRedisProperties properties,
            Environment environment) {
        GenericJackson2JsonRedisSerializer jsonSerializer = jsonSerializer();

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.getDefaultTtl())
                .disableCachingNullValues()
                .prefixCacheNameWith(properties.getKeyPrefix() + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        register(perCache, defaults, CacheNames.PRODUCTS, properties);
        register(perCache, defaults, CacheNames.PRODUCT_BY_ID, properties);
        register(perCache, defaults, CacheNames.USER_BY_USERNAME, properties);
        register(perCache, defaults, CacheNames.BUSINESS_BY_ID, properties);
        register(perCache, defaults, CacheNames.BUSINESSES_BY_OWNER, properties);
        register(perCache, defaults, CacheNames.STORE_BY_ID, properties);
        register(perCache, defaults, CacheNames.STORES_BY_BUSINESS, properties);
        register(perCache, defaults, CacheNames.STORAGE_FILE_BY_RELATIVE_PATH, properties);

        properties.getCaches().forEach((name, ttl) ->
                perCache.putIfAbsent(name, defaults.entryTtl(ttl != null ? ttl : properties.getDefaultTtl())));

        CacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
        if (!properties.isLogEvents()) {
            return redisCacheManager;
        }
        return new CacheEventLoggingCacheManager(
                redisCacheManager,
                environment.getProperty("spring.application.name", "unknown"));
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisCacheService redisCacheService(
            RedisTemplate<String, Object> xRedisTemplate,
            XRedisProperties properties) {
        return new RedisCacheService(xRedisTemplate, properties.getKeyPrefix(), properties.getDefaultTtl());
    }

    static GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Hibernate6Module hibernate6Module = new Hibernate6Module();
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        hibernate6Module.configure(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true);
        mapper.registerModule(hibernate6Module);

        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    private static void register(
            Map<String, RedisCacheConfiguration> map,
            RedisCacheConfiguration defaults,
            String cacheName,
            XRedisProperties properties) {
        Duration ttl = properties.getCaches().getOrDefault(cacheName, properties.getDefaultTtl());
        map.put(cacheName, defaults.entryTtl(ttl));
    }
}
