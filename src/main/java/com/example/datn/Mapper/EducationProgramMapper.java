package com.example.datn.Mapper;

import com.example.datn.DTO.Request.EducationProgramRequest;
import com.example.datn.DTO.Response.EducationProgramResponse;
import com.example.datn.Model.EducationProgram;
import com.example.datn.Model.Major;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EducationProgramMapper {


    public EducationProgram toEntity(EducationProgramRequest request, Major major) {
        if (request == null) {
            return null;
        }

        return EducationProgram.builder()
                .code(request.getCode())
                .name(request.getName())
                .totalCredits(request.getTotalCredits())
                .durationYears(request.getDurationYears())
//                .isTemplate(request.getIsTemplate())
                .major(major) // Lắp Object Major đã được Service tìm thấy vào đây
                .isActive(true) // Mặc định khi tạo mới là true
                .build();
    }


    public EducationProgramResponse toResponse(EducationProgram entity) {
        if (entity == null) {
            return null;
        }

        return EducationProgramResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .totalCredits(entity.getTotalCredits())
                .durationYears(entity.getDurationYears())
                .isTemplate(entity.getIsTemplate())
                .majorId(entity.getMajor() != null ? entity.getMajor().getId() : null)
                .majorName(entity.getMajor() != null ? entity.getMajor().getName() : null)
                .isActive(entity.getIsActive())
                .build();
    }
    public List<EducationProgramResponse> toResponseList(List<EducationProgram> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}