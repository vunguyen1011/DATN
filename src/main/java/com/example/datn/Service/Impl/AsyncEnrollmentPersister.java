package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import com.example.datn.Service.Interface.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEnrollmentPersister {

    private final EnrollmentSaveHelper enrollmentSaveHelper;
    private final IRedisService redisService;
    private final com.example.datn.Config.EnrollmentCacheManager enrollmentCacheManager;

    @Async("dbSaveExecutor")
    public void saveToDatabaseAsync(List<EnrollmentSaveRequest> requests, boolean isEnroll) {
        for (EnrollmentSaveRequest req : requests) {
            int attempt = 0;
            boolean success = false;
            final int maxRetries = 3;

            while (attempt < maxRetries && !success) {
                try {
                    enrollmentSaveHelper.saveOne(req, isEnroll);
                    success = true;
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException
                         | org.springframework.dao.DataIntegrityViolationException
                         | org.hibernate.StaleObjectStateException e) {

                    attempt++;
                    log.warn("[Retry {}/{}] Xung đột dữ liệu cho Enrollment {}. Đang thử lại...",
                            attempt, maxRetries, req.enrollmentId());

                    if (attempt >= maxRetries) {
                        log.error("[Async] Enroll THẤT BẠI — sv: {}, lớp: {}. Cần reconcile thủ công.",
                                req.studentId(), req.classSectionId());
                        if (isEnroll) {
                            redisService.releaseSlot(req.classSectionId(), req.studentId());
                        }
                    } else {
                        try {
                            // Exponential backoff nhỏ để tránh storm retry
                            Thread.sleep(50L * attempt);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception e) {
                    log.error("[Lỗi hệ thống] Không thể lưu Enrollment {}: {}", req.enrollmentId(), e.getMessage());
                    if (isEnroll) {
                        redisService.releaseSlot(req.classSectionId(), req.studentId());
                    }
                    break;
                }
            }
        }
        // Xóa cache sau khi tất cả đã được lưu xong vào DB
        if (!requests.isEmpty()) {
            EnrollmentSaveRequest first = requests.get(0);
            enrollmentCacheManager.evictEnrolledSections(first.studentId());
        }
    }
}