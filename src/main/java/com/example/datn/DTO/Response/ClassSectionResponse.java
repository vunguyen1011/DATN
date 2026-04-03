package com.example.datn.DTO.Response;

import com.example.datn.ENUM.SectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSectionResponse {
    private UUID id;
    private String sectionCode;
    private String courseGroupCode;
    private UUID subjectComponentId;
    private String subjectComponentType;
    private String subjectName;
    private String subjectCode;
    private UUID parentSectionId;
    private UUID semesterId;
    private Integer capacity;
    private Integer minStudents;
    private Integer enrolledCount;
    private SectionStatus status;
}
