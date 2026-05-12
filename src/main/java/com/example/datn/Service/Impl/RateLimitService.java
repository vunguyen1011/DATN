package com.example.datn.Service.Impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ProxyManager<String> proxyManager;

    // 1. Tạo một Record (DTO) để chứa kết quả trả về cho Filter/Interceptor
    public record RateLimitResult(boolean isAllowed, long remainingTokens, long nanosToWaitForRefill) {}

    /**
     * Hàm linh hoạt dùng cho cả Filter (vòng ngoài) và Interceptor (vòng trong)
     */
    public RateLimitResult tryConsumeDetailed(String key, int maxRequests, int windowInSeconds) {
        Supplier<BucketConfiguration> configSupplier = () -> {
            // Sử dụng GREEDY để nạp token mượt mà theo thời gian thực
            Refill refill = Refill.greedy(maxRequests, Duration.ofSeconds(windowInSeconds));
            Bandwidth limit = Bandwidth.classic(maxRequests, refill);
            return BucketConfiguration.builder().addLimit(limit).build();
        };

        var bucket = proxyManager.builder().build(key, configSupplier);

        // Dùng probe để lấy ra toàn bộ thông tin thay vì chỉ lấy boolean
        var probe = bucket.tryConsumeAndReturnRemaining(1);

        return new RateLimitResult(
                probe.isConsumed(),
                probe.getRemainingTokens(),
                probe.getNanosToWaitForRefill()
        );
    }
}