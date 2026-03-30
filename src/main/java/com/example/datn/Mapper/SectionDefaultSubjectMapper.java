package com.example.datn.Mapper;

import com.example.datn.DTO.Request.SectionDefaultSubjectRequest;
import com.example.datn.DTO.Response.SectionDefaultSubjectResponse;
import com.example.datn.Model.SectionDefault;
import com.example.datn.Model.SectionDefaultSubject;
import com.example.datn.Model.Subject;
import org.springframework.stereotype.Component;

@Component
public class SectionDefaultSubjectMapper {

    public SectionDefaultSubject toEntity(SectionDefaultSubjectRequest request, SectionDefault sectionDefault, Subject subject) {
        if (request == null) {
            return null;
        }

        return SectionDefaultSubject.builder()
                .sectionDefault(sectionDefault)
                .subject(subject)
                .defaultSemester(request.getDefaultSemester())
                .isActive(true)
                .build();
    }

    public SectionDefaultSubjectResponse toResponse(SectionDefaultSubject entity) {
        if (entity == null) {
            return null;
        }

        return SectionDefaultSubjectResponse.builder()
                .id(entity.getId())
                .sectionDefaultId(entity.getSectionDefault() != null ? entity.getSectionDefault().getId() : null)
                .subjectId(entity.getSubject() != null ? entity.getSubject().getId() : null)
                .subjectCode(entity.getSubject() != null ? entity.getSubject().getCode() : null)
                .subjectName(entity.getSubject() != null ? entity.getSubject().getName() : null)
                .subjectCredits(entity.getSubject() != null ? entity.getSubject().getCredits() : null)
                .defaultSemester(entity.getDefaultSemester())
                .isActive(entity.getIsActive())
                .build();
    }
}