package com.example.datn.Mapper;

import com.example.datn.DTO.Request.RegistrationPeriodRequest;
import com.example.datn.DTO.Response.RegistrationPeriodResponse;
import com.example.datn.Model.RegistrationPeriod;
import com.example.datn.Model.Semester;
import org.springframework.stereotype.Component;

@Component
public class RegistrationPeriodMapper {

    public RegistrationPeriod toEntity(RegistrationPeriodRequest request, Semester semester) {
        return RegistrationPeriod.builder()
                .semester(semester)
                .name(request.getName())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }

    public RegistrationPeriodResponse toResponse(RegistrationPeriod entity) {
        return RegistrationPeriodResponse.builder()
                .id(entity.getId())
                .semesterId(entity.getSemester() != null ? entity.getSemester().getId() : null)
                .semesterName(entity.getSemester() != null ? entity.getSemester().getName() : null)
                .name(entity.getName())
                .isActive(entity.getIsActive())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromRequest(RegistrationPeriod entity, RegistrationPeriodRequest request, Semester semester) {
        entity.setSemester(semester);
        entity.setName(request.getName());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
}
