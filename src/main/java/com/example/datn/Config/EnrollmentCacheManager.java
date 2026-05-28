package com.example.datn.Config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bean chuyên biệt để xử lý Cache Eviction cho luồng đăng ký học phần.
 *
 * LÝ DO tách ra bean riêng:
 * Spring Cache dùng AOP Proxy. Nếu @CacheEvict được gọi từ bên trong cùng một class (self-invocation),
 * Spring sẽ KHÔNG đi qua proxy → @CacheEvict bị bỏ qua hoàn toàn (silent failure).
 * Giải pháp chuẩn: đặt @CacheEvict trong một Spring-managed bean khác.
 */
@Component
public class EnrollmentCacheManager {

    /**
     * Xóa cache danh sách lớp đã đăng ký của sinh viên trong học kỳ.
     * Gọi sau khi enroll() hoặc cancelEnrollment() để đảm bảo lần đọc tiếp theo lấy dữ liệu mới.
     *
     * @param studentId  ID của sinh viên
     * @param semesterId ID của học kỳ
     */
    @CacheEvict(value = "enrolledSections", key = "#studentId", cacheManager = "redisCacheManager")
    public void evictEnrolledSections(UUID studentId) {
        // Phương thức này chỉ có nhiệm vụ trigger @CacheEvict qua Spring AOP proxy
    }
}
