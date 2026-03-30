package com.example.datn.Mapper;

import com.example.datn.DTO.Request.AcademicYearRequest;
import com.example.datn.Model.AcademicYear;
import org.springframework.stereotype.Component;

@Component
public class AcademicYearMapper {
    public AcademicYear toEntity(AcademicYearRequest request) {
        if (request == null) {
            return null;
        }
        return AcademicYear.builder()
                .name(request.getName().trim())
                .isCurrent(request.getIsCurrent())
                .build();
    }
    public void updateAcademicYearFromRequest(AcademicYear entity, AcademicYearRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            entity.setName(request.getName().trim());
        }
        if (request.getIsCurrent() != null) {
            entity.setIsCurrent(request.getIsCurrent());
        }
    }
}