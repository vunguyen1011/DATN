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
public class ClassSectionCacheDTO {
    private UUID id;
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private UUID semesterId;
    private UUID parentSectionId;
    private boolean hasLab;
}
