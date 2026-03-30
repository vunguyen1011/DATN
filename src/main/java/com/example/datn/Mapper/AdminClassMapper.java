package com.example.datn.Mapper;

import com.example.datn.DTO.Request.AdminClassRequest;
import com.example.datn.DTO.Response.AdminClassResponse;
import com.example.datn.Model.AdminClass;
import com.example.datn.Model.Cohort;
import com.example.datn.Model.Major;
import org.springframework.stereotype.Component;

@Component
public class AdminClassMapper {

    // 1. Map Request -> Entity (Cho Create)
    public AdminClass toEntity(AdminClassRequest request, Major major, Cohort cohort) {
        if (request == null) return null;
        return AdminClass.builder()
                .name(request.getName().trim())
                .major(major)
                .cohort(cohort)
                .build();
    }

    // 2. Map Entity -> Response (Cho trả về Frontend)
    public AdminClassResponse toResponse(AdminClass entity) {
        if (entity == null) return null;
        return AdminClassResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .majorId(entity.getMajor() != null ? entity.getMajor().getId() : null)
                .majorName(entity.getMajor() != null ? entity.getMajor().getName() : null)
                .cohortId(entity.getCohort() != null ? entity.getCohort().getId() : null)
                .cohortName(entity.getCohort() != null ? entity.getCohort().getName() : null)
                .build();
    }

    // 3. Update Entity (Cho Edit)
    public void updateAdminClassFromRequest(AdminClass entity, AdminClassRequest request, Major major, Cohort cohort) {
        if (entity == null || request == null) return;

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            entity.setName(request.getName().trim());
        }

        if (major != null) {
            entity.setMajor(major);
        }
        if (cohort != null) {
            entity.setCohort(cohort);
        }
    }
}