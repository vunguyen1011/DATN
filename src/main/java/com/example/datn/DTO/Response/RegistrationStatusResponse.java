package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationStatusResponse {
    private boolean isEligible;
    private String message;
    private String periodName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String semesterName;
}
