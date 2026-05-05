package com.example.datn.DTO.Response;

import com.example.datn.ENUM.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnrollmentSimpleResponse {
    private UUID id;
    private EnrollmentStatus status;
    private LocalDateTime enrollmentDate;
    private String sectionCode;
    private String subjectName;
}
