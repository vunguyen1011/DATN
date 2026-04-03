package com.example.datn.Mapper;

import com.example.datn.DTO.Response.ClassSectionResponse;
import com.example.datn.Model.ClassSection;
import org.springframework.stereotype.Component;

@Component
public class ClassSectionMapper {
    public ClassSectionResponse toResponse(ClassSection entity) {
        if (entity == null) {
            return null;
        }

        return ClassSectionResponse.builder()
                .id(entity.getId())
                .sectionCode(entity.getSectionCode())
                .courseGroupCode(entity.getCourseGroupCode())
                .subjectComponentId(entity.getSubjectComponent() != null ? entity.getSubjectComponent().getId() : null)
                .subjectComponentType(entity.getSubjectComponent() != null && entity.getSubjectComponent().getType() != null ? entity.getSubjectComponent().getType().name() : null)
                .subjectName(entity.getSubjectComponent() != null && entity.getSubjectComponent().getSubject() != null ? entity.getSubjectComponent().getSubject().getName() : null)
                .subjectCode(entity.getSubjectComponent() != null && entity.getSubjectComponent().getSubject() != null ? entity.getSubjectComponent().getSubject().getCode() : null)
                .parentSectionId(entity.getParentSection() != null ? entity.getParentSection().getId() : null)
                .semesterId(entity.getSemester() != null ? entity.getSemester().getId() : null)
                .capacity(entity.getCapacity())
                .minStudents(entity.getMinStudents())
                .enrolledCount(entity.getEnrolledCount())
                .status(entity.getStatus())
                .build();
    }
}
