package com.tabcorp.transaction.management.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration class that disables Redis caching for integration tests.
 * 
 * This configuration overrides the default cache configuration to set the cache type to NONE,
 * which prevents any actual caching from occurring during tests. This is useful for integration
 * tests where we want to test the service logic without the caching layer.
 */
@TestConfiguration
@EnableCaching
public class TestCacheConfiguration {

    /**
     * Creates a CacheProperties bean with cache type set to NONE.
     * 
     * This bean takes precedence over any other CacheProperties bean due to @Primary annotation,
     * effectively disabling the Redis cache for tests where this configuration is used.
     * 
     * @return CacheProperties with cache type set to NONE
     */
    @Bean
    @Primary
    public CacheProperties cacheProperties() {
        CacheProperties properties = new CacheProperties();
        properties.setType(CacheType.NONE);
        return properties;
    }
}

