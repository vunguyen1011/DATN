package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import com.example.datn.Repository.ClassSectionRepository;
import com.example.datn.Repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentSaveHelper {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassSectionRepository classSectionRepository;

    /**
     * Nhận DTO thuần túy (không phải JPA entity) để tránh hoàn toàn
     * StaleObjectStateException khi vượt ranh giới thread.
     *
     * Dùng native upsert thay vì findById + saveAndFlush:
     *  - Tránh DataIntegrityViolationException khi 2 luồng cùng INSERT cùng (student, class)
     *  - Tránh OptimisticLockException vì không đụng đến @Version của bất kỳ entity nào
     *  - Toàn bộ operation là 2 native SQL atomic: không Hibernate object lifecycle
     *
     * Mỗi lần gọi mở một transaction độc lập (REQUIRES_NEW).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOne(EnrollmentSaveRequest req, boolean isEnroll) {
        // 1. Cập nhật sĩ số bằng native SQL (atomic, KHÔNG đụng @Version của ClassSection)
        if (isEnroll) {
            classSectionRepository.tryIncrementEnrolledCount(req.classSectionId());
        } else {
            classSectionRepository.tryDecrementEnrolledCount(req.classSectionId());
        }

        // 2. Upsert enrollment nguyên tử — tránh race condition INSERT và unique violation
        //    ON CONFLICT (student_id, class_section_id) DO UPDATE → idempotent
        enrollmentRepository.upsertEnrollment(
                req.enrollmentId(),
                req.studentId(),
                req.classSectionId(),
                req.status().name(),   // enum → String cho native SQL
                req.enrollmentDate()
        );
    }
}