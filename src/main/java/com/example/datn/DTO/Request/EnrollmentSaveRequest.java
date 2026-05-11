package com.example.datn.DTO.Request;

import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.Model.Enrollment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO thuần túy (không phải JPA entity) dùng để truyền thông tin enrollment
 * qua ranh giới thread mà không bị ràng buộc bởi Hibernate Session.
 * Tránh hoàn toàn vấn đề StaleObjectStateException / LazyInitializationException.
 */
public record EnrollmentSaveRequest(
        UUID enrollmentId,
        UUID studentId,
        UUID classSectionId,
        UUID semesterId,
        EnrollmentStatus status,
        LocalDateTime enrollmentDate
) {

    public static EnrollmentSaveRequest from(Enrollment e) {
        return new EnrollmentSaveRequest(
                e.getId(),
                e.getStudent().getId(),
                e.getClassSection().getId(),
                e.getClassSection().getSemester().getId(),
                e.getStatus(),
                e.getEnrollmentDate()
        );
    }
}
