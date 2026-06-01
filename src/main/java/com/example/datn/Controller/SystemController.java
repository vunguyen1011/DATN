package com.example.datn.Controller;

import com.example.datn.Service.Interface.IRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System Utilities", description = "Các chức năng quản trị hệ thống")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final CacheManager cacheManager;
    private final IRedisService redisService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.example.datn.Service.Interface.IWarmupCacheService warmupCacheService;

    @Operation(summary = "Khởi tạo dữ liệu Đăng ký tín chỉ", description = "Tính toán sẵn bitmask, môn đã qua, môn tiên quyết và lưu vào Redis")
    @org.springframework.web.bind.annotation.PostMapping("/warmup-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> warmupCache() {
        warmupCacheService.warmupAll();
        return ResponseEntity.ok("Đã chạy tiến trình Warmup Cache thành công!");
    }

    @Operation(summary = "Xóa cache của hệ thống", description = "Thực hiện xóa toàn bộ Cache Redis (chỉ xóa cache Spring, giữ lại Token/Session)")
    @DeleteMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearCache() {
        // CacheManager.getCacheNames() thường bị rỗng sau khi khởi động lại app.
        // Nên ta dùng lệnh quét key của Redis để xóa sạch các key của Spring Cache (chứa '::')
        try {
            java.util.Set<String> keys = redisTemplate.keys("*::*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // Fallback
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
        return ResponseEntity.ok("Đã xóa toàn bộ bộ nhớ Cache");
    }

    @Operation(summary = "Xóa lock đăng ký trên Redis", description = "Dùng khi DB và Redis bị lệch data (DB lưu thất bại nhưng Redis vẫn hold slot). Xóa các key class_slot, class_students, student_subject.")
    @DeleteMapping("/registration-locks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearRegistrationLocks() {
        redisService.clearRegistrationData();
        return ResponseEntity.ok("Đã xóa toàn bộ lock đăng ký trên Redis");
    }
}
