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
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Qualifier("dbSaveExecutor")
    private final Executor dbSaveExecutor;

    @Async("dbSaveExecutor")
    public void saveToDatabaseAsync(List<EnrollmentSaveRequest> requests, boolean isEnroll) {
        if (requests == null || requests.isEmpty()) return;
        persistBatchWithRetry(requests, isEnroll, 0);
    }

    private void persistBatchWithRetry(List<EnrollmentSaveRequest> requests, boolean isEnroll, int attempt) {
        try {
            // Lưu toàn bộ Theory và Lab trong 1 transaction (Atomic)
            enrollmentSaveHelper.saveBatch(requests);
            
            // Xóa cache sau khi lưu thành công cho sinh viên đầu tiên (vì tất cả chung 1 SV)
            if (!requests.isEmpty()) {
                enrollmentCacheManager.evictEnrolledSections(requests.get(0).studentId());
            }
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException
                 | org.springframework.dao.DataIntegrityViolationException
                 | org.hibernate.StaleObjectStateException e) {

            int nextAttempt = attempt + 1;
            final int maxRetries = 3;
            
            java.util.UUID studentId = requests.isEmpty() ? null : requests.get(0).studentId();

            log.warn("[Retry {}/{}] Xung đột dữ liệu khi lưu batch cho SV {}. Đang thử lại...",
                    nextAttempt, maxRetries, studentId);

            if (nextAttempt >= maxRetries) {
                log.error("[Async] Enroll THẤT BẠI cho SV: {}. Cần reconcile thủ công.", studentId);
                if (isEnroll) {
                    // Rollback toàn bộ slot trên Redis
                    for (EnrollmentSaveRequest req : requests) {
                        String classMaskStr = redisTemplate.opsForValue().get("class_mask:" + req.classSectionId());
                        redisService.releaseSlot(req.classSectionId(), req.studentId(), req.subjectId(), classMaskStr);
                    }
                }
                if (studentId != null) {
                    enrollmentCacheManager.evictEnrolledSections(studentId);
                }
            } else {
                long delayMs = 50L * nextAttempt;
                CompletableFuture.runAsync(
                        () -> persistBatchWithRetry(requests, isEnroll, nextAttempt),
                        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS, dbSaveExecutor)
                );
            }
        } catch (Exception e) {
            java.util.UUID studentId = requests.isEmpty() ? null : requests.get(0).studentId();
            log.error("[Lỗi hệ thống] Không thể lưu batch Enrollment cho SV {}: {}", studentId, e.getMessage());
            if (isEnroll) {
                for (EnrollmentSaveRequest req : requests) {
                    String classMaskStr = redisTemplate.opsForValue().get("class_mask:" + req.classSectionId());
                    redisService.releaseSlot(req.classSectionId(), req.studentId(), req.subjectId(), classMaskStr);
                }
            }
            if (studentId != null) {
                enrollmentCacheManager.evictEnrolledSections(studentId);
            }
        }
    }
}