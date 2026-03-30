package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectGroupSectionResponse {
    private UUID id;
    private String title;
    private Integer index;
    private String note;
    private Boolean isMandatory;
    private Integer requiredCredits;
    private Boolean isActive;

    private UUID educationProgramId;
    private UUID subjectGroupId;


    private Integer totalCredits;
}