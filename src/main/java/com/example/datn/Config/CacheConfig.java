package com.example.datn.Config; // Đã đổi thành viết thường theo chuẩn naming convention

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
        @Bean
        @Primary
        public CacheManager caffeineCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                                "ongoingCohortPeriod",
                                "prerequisites",
                                "registrationStatus",
                                "classSection", 
                                "classSectionSchedules",
                                "enrolledSections", 
                                "passedSubjects", 
                                "scheduleOverlap"
                );
                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .expireAfterWrite(1, TimeUnit.MINUTES) // Giảm xuống 1 phút để dữ liệu luôn mới
                                .maximumSize(1000));
                return cacheManager;
        }

        /**
         * Redis Cache - dùng cho các DTO/dữ liệu cần chia sẻ giữa nhiều instance.
         * Chỉ cache những object đã được serialize an toàn (không phải JPA proxy).
         */
        @Bean
        @SuppressWarnings("deprecation")
        public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
                RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                .disableCachingNullValues();

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(config)
                                .build();
        }
}