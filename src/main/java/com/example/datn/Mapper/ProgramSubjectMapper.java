package com.example.datn.Mapper;

import com.example.datn.DTO.Request.ProgramSubjectRequest;
import com.example.datn.DTO.Response.ProgramSubjectResponse;
import com.example.datn.Model.ProgramSubject;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectGroupSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProgramSubjectMapper {

    public ProgramSubjectResponse toResponse(ProgramSubject ps) {
        if (ps == null) return null;
        return ProgramSubjectResponse.builder()
                .id(ps.getId())
                .subjectId(ps.getSubject().getId())
                .subjectCode(ps.getSubject().getCode())
                .subjectName(ps.getSubject().getName())
                .credits(ps.getSubject().getCredits())
                .sectionId(ps.getSection().getId())
                .sectionTitle(ps.getSection().getTitle())
                .defaultSemester(ps.getSemester())
                .weight(ps.getWeight())
                .isActive(ps.getIsActive())
                .build();
    }

    public List<ProgramSubjectResponse> toResponseList(List<ProgramSubject> list) {
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProgramSubject toEntity(ProgramSubjectRequest request, Subject subject, SubjectGroupSection section) {
        return ProgramSubject.builder()
                .subject(subject)
                .section(section)
                .semester(request.getDefaultSemester())
                .weight(request.getWeight())
                .isActive(true)
                .build();
    }

    public void updateEntity(ProgramSubject ps, ProgramSubjectRequest request, Subject subject, SubjectGroupSection section) {
        ps.setSubject(subject);
        ps.setSection(section);
        ps.setSemester(request.getDefaultSemester());
        ps.setWeight(request.getWeight());
    }
}