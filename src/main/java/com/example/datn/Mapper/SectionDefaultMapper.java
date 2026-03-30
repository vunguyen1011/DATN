package com.example.datn.Mapper;

import com.example.datn.DTO.Request.SectionDefaultRequest;
import com.example.datn.DTO.Response.SectionDefaultResponse;
import com.example.datn.Model.SectionDefault;
import com.example.datn.Model.SubjectGroup;
import org.springframework.stereotype.Component;

@Component
public class SectionDefaultMapper {

    public SectionDefault toEntity(SectionDefaultRequest request, SubjectGroup subjectGroup) {
        if (request == null) {
            return null;
        }

        return SectionDefault.builder()
                .title(request.getTitle())
                .isMandatory(request.getIsMandatory())
                .requiredCredits(request.getRequiredCredits())
                .index(request.getIndex())
                .subjectGroup(subjectGroup)
                .build();
    }

    public SectionDefaultResponse toResponse(SectionDefault entity) {
        if (entity == null) {
            return null;
        }

        return SectionDefaultResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .isMandatory(entity.getIsMandatory())
                .requiredCredits(entity.getRequiredCredits())
                .index(entity.getIndex())
                .subjectGroupId(entity.getSubjectGroup() != null ? entity.getSubjectGroup().getId() : null)
                .note(entity.getNote())
                .build();
    }
}