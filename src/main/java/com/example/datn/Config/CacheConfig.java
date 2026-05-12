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

    /**
     * Caffeine Cache (in-memory) - KHÔNG cần serialize sang JSON.
     * Dùng cho cache các JPA Entity từ Repository (vd: PeriodCohort, Prerequisite).
     * Tránh hoàn toàn lỗi HibernateProxy + Jackson serialization.
     * Được đặt là @Primary để các @Cacheable mặc định dùng cache này.
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "ongoingCohortPeriod",
                "prerequisites",
                "registrationStatus",
                "classSection",          // Cache lớp học phần theo ID (dữ liệu tĩnh, không đổi trong kỳ)
                "enrolledSections",      // Cache danh sách lớp đã đăng ký của SV trong học kỳ
                "passedSubjects",        // Cache các môn SV đã qua — rất tĩnh, không cần invalidate thường xuyên
                "scheduleOverlap"        // Cache kết quả kiểm tra trùng lịch (theo cặp sectionId)
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
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}