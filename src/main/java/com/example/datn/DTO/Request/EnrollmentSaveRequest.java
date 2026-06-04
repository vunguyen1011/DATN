package com.example.datn.DTO.Request;

import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.Model.Enrollment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnrollmentSaveRequest(
    UUID enrollmentId,
    UUID studentId,
    UUID classSectionId,
    UUID semesterId,
    UUID subjectId,
    EnrollmentStatus oldStatus,
    EnrollmentStatus newStatus,
    LocalDateTime enrollmentDate
) {
    public static EnrollmentSaveRequest from(Enrollment e) {
        EnrollmentStatus oldStatus = (e.getStatus() == EnrollmentStatus.CANCELLED) ? EnrollmentStatus.REGISTERED : null;
        return new EnrollmentSaveRequest(
                e.getId(),
                e.getStudent().getId(),
                e.getClassSection().getId(),
                e.getClassSection().getSemester().getId(),
                e.getClassSection().getSubject().getId(),
                oldStatus,
                e.getStatus(),
                e.getEnrollmentDate()
        );
    }
}
