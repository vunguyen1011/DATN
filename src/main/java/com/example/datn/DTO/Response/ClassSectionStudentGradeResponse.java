package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ClassSectionStudentGradeResponse {
    private UUID enrollmentId;
    private UUID studentId;
    private String studentCode;
    private String studentName;
    private String adminClassName;
    private Double midtermScore; // Điểm quá trình
    private Double finalScore;   // Điểm thi
    private Double totalScore;   // Điểm tổng kết
    private String letterGrade;  // Điểm chữ
    private Boolean isPassed;
}
