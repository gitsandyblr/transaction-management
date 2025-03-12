package com.tabcorp.transaction.management.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration for Redis caching
 */
@Configuration
public class CacheConfig {

    @Value("${spring.cache.redis.time-to-live:300000}")
    private long timeToLiveMs;

    @Bean
    public CacheManager cacheManager(@Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory connectionFactory) {
        // Get the underlying Lettuce connection factory
        RedisConnectionFactory redisConnectionFactory;
        if (connectionFactory instanceof LettuceConnectionFactory) {
            // Get the same configuration but as a non-reactive connection factory
            redisConnectionFactory = (LettuceConnectionFactory) connectionFactory;
        } else {
            throw new IllegalStateException("ReactiveRedisConnectionFactory is not a LettuceConnectionFactory");
        }
        
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(timeToLiveMs))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();
    }
}

