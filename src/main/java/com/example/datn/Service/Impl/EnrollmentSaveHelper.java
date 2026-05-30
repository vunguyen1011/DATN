package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import com.example.datn.Repository.ClassSectionRepository;
import com.example.datn.Repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentSaveHelper {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassSectionRepository classSectionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOne(EnrollmentSaveRequest req) {
        saveInternal(req);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(java.util.List<EnrollmentSaveRequest> requests) {
        for (EnrollmentSaveRequest req : requests) {
            saveInternal(req);
        }
    }

    private void saveInternal(EnrollmentSaveRequest req) {
        boolean isEnroll = req.status() == com.example.datn.ENUM.EnrollmentStatus.REGISTERED;
        if (isEnroll) {
            int updatedRows = classSectionRepository.tryIncrementEnrolledCount(req.classSectionId());
            if (updatedRows == 0) {
                // BẮT BUỘC ném AppException để Spring Rollback lại toàn bộ transaction
                throw new AppException(ErrorCode.INVALID_REQUEST, "DB Rejection: Sĩ số đã đầy hoặc lớp không tồn tại " + req.classSectionId());
            }
        } else {
            int updatedRows = classSectionRepository.tryDecrementEnrolledCount(req.classSectionId());
            if (updatedRows == 0) {
                log.warn("Không thể giảm sĩ số cho lớp {} (có thể sĩ số đã = 0)", req.classSectionId());
            }
        }
        enrollmentRepository.upsertEnrollment(
                req.enrollmentId(),
                req.studentId(),
                req.classSectionId(),
                req.status().name(),   // enum → String cho native SQL
                req.enrollmentDate()
        );
    }
}