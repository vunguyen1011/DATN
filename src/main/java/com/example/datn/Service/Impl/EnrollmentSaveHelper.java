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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOne(EnrollmentSaveRequest req, boolean isEnroll) {
        if (isEnroll) {
            classSectionRepository.tryIncrementEnrolledCount(req.classSectionId());
        } else {
            classSectionRepository.tryDecrementEnrolledCount(req.classSectionId());
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