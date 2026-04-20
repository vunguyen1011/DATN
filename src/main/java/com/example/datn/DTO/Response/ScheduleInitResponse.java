package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ScheduleInitResponse {
    private UUID semesterId;
    private String semesterName;
    private long totalSections;
    private long alreadyHadSchedule;
    private long newlyCreated;
    private String message;
}