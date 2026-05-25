package com.example.datn.Controller;

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

    @Operation(summary = "Xóa cache của hệ thống", description = "Thực hiện xóa toàn bộ Cache Redis/Caffeine")
    @DeleteMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearCache() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        return ResponseEntity.ok("Đã xóa toàn bộ bộ nhớ Cache");
    }
}
