package com.example.datn.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentGradeResponse {

    private UUID gradeId;

    // Thông tin enrollment
    private UUID enrollmentId;
    private UUID studentId;
    private String studentCode;
    private String studentName;

    // Thông tin môn học
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;

    // Thông tin kỳ học
    private UUID semesterId;
    private String semesterName;

    // Điểm
    private Double midtermScore;
    private Double finalScore;
    private Double totalScore;

    // Quan trọng nhất cho đăng ký học
    private Boolean isPassed;
}
