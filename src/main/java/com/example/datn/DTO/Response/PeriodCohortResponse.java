package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodCohortResponse {
    private UUID id;
    private UUID registrationPeriodId;
    private String registrationPeriodName;
    private UUID cohortId;
    private String cohortName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
