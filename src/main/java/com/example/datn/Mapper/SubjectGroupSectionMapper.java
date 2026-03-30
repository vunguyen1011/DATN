package com.example.datn.Mapper;

import com.example.datn.DTO.Request.SubjectGroupSectionRequest;
import com.example.datn.DTO.Response.SubjectGroupSectionResponse;
import com.example.datn.Model.SubjectGroupSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubjectGroupSectionMapper {

    // Chuyển Request -> Entity (Dùng cho tạo mới thủ công nếu cần)
    public SubjectGroupSection toEntity(SubjectGroupSectionRequest request) {
        if (request == null) return null;

        return SubjectGroupSection.builder()
                .title(request.getTitle())
                .index(request.getIndex())
                .note(request.getNote())
                .isMandatory(request.getIsMandatory())
                .requiredCredits(request.getRequiredCredits())
                .isActive(true)
                .build();
    }

    // Cập nhật Entity từ Request
    public void updateEntity(SubjectGroupSection section, SubjectGroupSectionRequest request) {
        if (section == null || request == null) return;

        section.setTitle(request.getTitle());
        section.setIndex(request.getIndex());
        section.setNote(request.getNote());
        section.setIsMandatory(request.getIsMandatory());
        section.setRequiredCredits(request.getRequiredCredits());
    }

    // Chuyển Entity -> Response
    public SubjectGroupSectionResponse toResponse(SubjectGroupSection section) {
        if (section == null) return null;

        // Logic hiển thị note tự động nếu Admin để trống
        String displayNote = section.getNote();
        if (displayNote == null || displayNote.trim().isEmpty()) {
            displayNote = Boolean.TRUE.equals(section.getIsMandatory())
                    ? "Bắt buộc học tất cả các môn"
                    : "Chọn tối thiểu " + section.getRequiredCredits() + " tín chỉ trong khối này";
        }

        return SubjectGroupSectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .index(section.getIndex())
                .isMandatory(section.getIsMandatory())
                .requiredCredits(section.getRequiredCredits())
                .educationProgramId(section.getEducationProgram() != null ? section.getEducationProgram().getId() : null)
                .subjectGroupId(section.getSubjectGroup() != null ? section.getSubjectGroup().getId() : null)
                .isActive(section.getIsActive())
                .note(displayNote)
                .build();
    }

    public List<SubjectGroupSectionResponse> toResponseList(List<SubjectGroupSection> sections) {
        if (sections == null) return List.of();
        return sections.stream().map(this::toResponse).collect(Collectors.toList());
    }
}