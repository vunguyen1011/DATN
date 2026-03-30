package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SectionDefaultResponse {
    private UUID id;
    private String title;
    private Boolean isMandatory;
    private Integer requiredCredits;
    private Integer index;
    private UUID subjectGroupId;
    private String note;

}