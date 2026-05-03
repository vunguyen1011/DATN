package com.example.datn.DTO.Response;

import com.example.datn.ENUM.EnrollmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EnrollmentResponse {
    private UUID id;
    private ClassSectionResponse classSection;
    private LocalDateTime enrollmentDate;
    private EnrollmentStatus status;
}
