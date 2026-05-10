package com.example.datn.Task;

import com.example.datn.ENUM.RecommendationStatus;
import com.example.datn.Repository.AiRecommendationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationCleanupJob {

    private final AiRecommendationHistoryRepository repository;

    @Scheduled(cron = "0 0 2 * * SUN") // Run at 2 AM every Sunday
    public void cleanupDeprecatedRecommendations() {
        log.info("Bắt đầu dọn dẹp các bản ghi AI Recommendation cũ...");
        try {
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            repository.deleteByStatusAndCreatedAtBefore(RecommendationStatus.DEPRECATED, sixMonthsAgo);
            log.info("Dọn dẹp thành công các bản ghi AI Recommendation DEPRECATED trước {}", sixMonthsAgo);
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp AI Recommendation", e);
        }
    }
}
