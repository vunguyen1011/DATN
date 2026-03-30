package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SectionDefaultSubjectResponse {
    private UUID id;
    private UUID sectionDefaultId;
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer subjectCredits;
    private Integer defaultSemester;
    private Boolean isActive;
}