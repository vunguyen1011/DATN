package com.example.datn.Mapper;

import com.example.datn.DTO.Request.SemesterRequest;
import com.example.datn.DTO.Response.SemesterResponse;
import com.example.datn.Model.AcademicYear;
import com.example.datn.Model.Semester;
import org.springframework.stereotype.Component;

@Component
public class SemesterMapper {

    public Semester toEntity(SemesterRequest request, AcademicYear academicYear) {
        if (request == null) return null;
        return Semester.builder()
                .name(request.getName().trim())
                .academicYear(academicYear)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
    }

    public SemesterResponse toResponse(Semester entity) {
        if (entity == null) return null;
        return SemesterResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                // Ép kiểu an toàn, tránh NullPointerException
                .academicYearId(entity.getAcademicYear() != null ? entity.getAcademicYear().getId() : null)
                .academicYearName(entity.getAcademicYear() != null ? entity.getAcademicYear().getName() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .build();
    }

    public void updateSemesterFromRequest(Semester entity, SemesterRequest request, AcademicYear academicYear) {
        if (entity == null || request == null) return;
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            entity.setName(request.getName().trim());
        }
        if (request.getStartDate() != null) {
            entity.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            entity.setEndDate(request.getEndDate());
        }
        if (academicYear != null) {
            entity.setAcademicYear(academicYear);
        }
    }
}