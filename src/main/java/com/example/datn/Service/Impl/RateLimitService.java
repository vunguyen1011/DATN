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
    public record RateLimitResult(boolean isAllowed, long remainingTokens, long nanosToWaitForRefill) {}
    public RateLimitResult tryConsumeDetailed(String key, int maxRequests, int windowInSeconds) {
        Supplier<BucketConfiguration> configSupplier = () -> {
            Refill refill = Refill.greedy(maxRequests, Duration.ofSeconds(windowInSeconds));
            Bandwidth limit = Bandwidth.classic(maxRequests, refill);
            return BucketConfiguration.builder().addLimit(limit).build();
        };
        var bucket = proxyManager.builder().build(key, configSupplier);
        var probe = bucket.tryConsumeAndReturnRemaining(1);
        return new RateLimitResult(
                probe.isConsumed(),
                probe.getRemainingTokens(),
                probe.getNanosToWaitForRefill()
        );
    }
}