package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    private UUID id;

    // ClassSection info
    private UUID classSectionId;
    private String sectionCode;
    private String subjectName;
    private String subjectCode;

    // Room info (nullable)
    private UUID roomId;
    private String roomName;

    // Lecturer info (nullable)
    private UUID lecturerId;
    private String lecturerName;
    private String lecturerCode;

    // Time info
    private Integer dayOfWeek;    // 2 -> 8
    private String dayOfWeekName; // "Thứ 2" -> "Chủ nhật"
    private Integer startPeriod;
    private Integer endPeriod;    // nullable
}
