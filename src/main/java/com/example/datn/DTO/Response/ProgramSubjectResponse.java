package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ProgramSubjectResponse {
    private UUID id;
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private UUID sectionId;
    private String sectionTitle;
    private Integer defaultSemester;
    private Double weight;
    private Boolean isActive;

}