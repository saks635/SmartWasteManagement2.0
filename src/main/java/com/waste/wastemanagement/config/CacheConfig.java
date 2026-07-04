package com.waste.wastemanagement.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Always-active caching config.
 * When Redis is disabled (local dev), Spring uses the 'simple' in-memory cache
 * configured via spring.cache.type=simple in application.properties.
 * When Redis is enabled, RedisConfig takes over and provides Redis-backed caching.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
