package com.apimarketplace.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
        RedisConnectionFactory connectionFactory,
        GenericJackson2JsonRedisSerializer redisSerializer,
        @Value("${app.cache.api-catalog-ttl-seconds:300}") long apiCatalogTtlSeconds,
        @Value("${app.cache.api-key-validation-ttl-seconds:300}") long apiKeyValidationTtlSeconds,
        @Value("${app.cache.provider-analytics-ttl-seconds:60}") long providerAnalyticsTtlSeconds
    ) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
            .entryTtl(Duration.ofSeconds(apiCatalogTtlSeconds));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new LinkedHashMap<>();
        cacheConfigurations.put("apiCatalogList", defaultConfig.entryTtl(Duration.ofSeconds(apiCatalogTtlSeconds)));
        cacheConfigurations.put("apiProductDetails", defaultConfig.entryTtl(Duration.ofSeconds(apiCatalogTtlSeconds)));
        cacheConfigurations.put("providerAnalytics", defaultConfig.entryTtl(Duration.ofSeconds(providerAnalyticsTtlSeconds)));
        cacheConfigurations.put("apiKeyPrefixCandidates", defaultConfig.entryTtl(Duration.ofSeconds(apiKeyValidationTtlSeconds)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

    @Bean
    public GenericJackson2JsonRedisSerializer redisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
