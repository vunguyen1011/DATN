package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class SubjectGradeTreeDTO {
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private Double midtermScore;
    private Double finalScore;
    private Double totalScore;
    private String letterGrade;
    private Boolean isPassed;
}
