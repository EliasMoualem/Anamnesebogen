package de.elias.moualem.Anamnesebogen.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for template generation and other cacheable operations.
 * Uses Caffeine as the cache implementation.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure CacheManager with Caffeine implementation.
     *
     * Cache Configuration:
     * - Maximum size: 100 entries per cache
     * - Expire after write: 1 hour
     * - Expire after access: 30 minutes
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("thymeleafTemplates");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats());

        return cacheManager;
    }
}
