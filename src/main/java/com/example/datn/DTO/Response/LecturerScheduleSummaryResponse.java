package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerScheduleSummaryResponse {
    private String lecturerCode;
    private String lecturerName;
    private int totalClasses;
    private int totalPeriods;
    private List<ScheduleDetail> schedules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDetail {
        private String subjectName;
        private String sectionCode;
        private Integer dayOfWeek;
        private String dayOfWeekName;
        private Integer startPeriod;
        private Integer endPeriod;
        private String roomName;
    }
}
