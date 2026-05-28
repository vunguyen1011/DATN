package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import com.example.datn.Service.Interface.IRedisService;
import com.example.datn.Config.EnrollmentCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEnrollmentPersister {

    private final EnrollmentSaveHelper enrollmentSaveHelper;
    private final IRedisService redisService;
    private final EnrollmentCacheManager enrollmentCacheManager;

    @Qualifier("dbSaveExecutor")
    private final Executor dbSaveExecutor;

    @Async("dbSaveExecutor")
    public void saveToDatabaseAsync(List<EnrollmentSaveRequest> requests, boolean isEnroll) {
        for (EnrollmentSaveRequest req : requests) {
            persistWithRetry(req, isEnroll, 0);
        }
    }

    private void persistWithRetry(EnrollmentSaveRequest req, boolean isEnroll, int attempt) {
        try {
            enrollmentSaveHelper.saveOne(req, isEnroll);
            // Xóa cache sau khi lưu thành công
            enrollmentCacheManager.evictEnrolledSections(req.studentId());
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException
                 | org.springframework.dao.DataIntegrityViolationException
                 | org.hibernate.StaleObjectStateException e) {

            int nextAttempt = attempt + 1;
            final int maxRetries = 3;

            log.warn("[Retry {}/{}] Xung đột dữ liệu cho Enrollment {}. Đang thử lại...",
                    nextAttempt, maxRetries, req.enrollmentId());

            if (nextAttempt >= maxRetries) {
                log.error("[Async] Enroll THẤT BẠI — sv: {}, lớp: {}. Cần reconcile thủ công.",
                        req.studentId(), req.classSectionId());
                if (isEnroll) {
                    redisService.releaseSlot(req.classSectionId(), req.studentId());
                }
                // Xóa cache để đảm bảo giao diện đồng bộ
                enrollmentCacheManager.evictEnrolledSections(req.studentId());
            } else {
                // Sử dụng CompletableFuture.delayedExecutor (Java 9+) để tạo delay không block Thread
                long delayMs = 50L * nextAttempt;
                CompletableFuture.runAsync(
                        () -> persistWithRetry(req, isEnroll, nextAttempt),
                        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS, dbSaveExecutor)
                );
            }
        } catch (Exception e) {
            log.error("[Lỗi hệ thống] Không thể lưu Enrollment {}: {}", req.enrollmentId(), e.getMessage());
            if (isEnroll) {
                redisService.releaseSlot(req.classSectionId(), req.studentId());
            }
            enrollmentCacheManager.evictEnrolledSections(req.studentId());
        }
    }
}