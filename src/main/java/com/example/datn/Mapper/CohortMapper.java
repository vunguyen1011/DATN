package com.example.datn.Mapper;

import com.example.datn.DTO.Request.CohortRequest;
import com.example.datn.Model.Cohort;
import org.springframework.stereotype.Component;

@Component
public class CohortMapper {
    public   Cohort toEntity(CohortRequest request) {
        if (request == null) {
            return null;
        }
        return Cohort.builder()
                .name(request.getName().trim())
                .startYear(request.getStartYear())
                .build();
    }

    public   void updateCohortFromRequest(Cohort entity, CohortRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            entity.setName(request.getName().trim());
        }

        if (request.getStartYear() != null) {
            entity.setStartYear(request.getStartYear());
        }
    }
}