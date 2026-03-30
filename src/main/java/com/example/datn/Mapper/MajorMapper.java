package com.example.datn.Mapper;

import com.example.datn.DTO.Request.MajorRequest;
import com.example.datn.Model.Major;
import org.springframework.stereotype.Component;

@Component
public class MajorMapper {
    public Major toEntity(MajorRequest request) {
        if (request == null) {
            return null;
        }
        return Major.builder()
                .name(request.getName())
                .code(request.getCode())
                .build();
    }
    public void updateMajorFromRequest(Major entity, MajorRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            entity.setName(request.getName().trim());
        }
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            entity.setCode(request.getCode().trim());
        }
    }


}
