package com.example.datn.Mapper;

import com.example.datn.DTO.Request.SubjectGroupRequest;
import com.example.datn.DTO.Response.SubjectGroupResponse;
import com.example.datn.Model.SubjectGroup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubjectGroupMapper {

    public SubjectGroupResponse toResponse(SubjectGroup group) {
        if (group == null) {
            return null;
        }

        return SubjectGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .isGlobal(group.getIsGlobal()) // Bổ sung cờ isGlobal để FE biết khối này có phải mặc định không
                .isActive(group.getIsActive())
                .index(group.getIndex())
                .build();
    }

    public List<SubjectGroupResponse> toResponseList(List<SubjectGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        return groups.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Đã xóa tham số EducationProgram program vì Khối Hồng giờ là độc lập
    public SubjectGroup toEntity(SubjectGroupRequest request) {
        if (request == null) {
            return null;
        }

        return SubjectGroup.builder()
                .name(request.getName())
                // Nếu request không truyền lên, mặc định sẽ là true (Dùng chung)
                .isGlobal(request.getIsGlobal() != null ? request.getIsGlobal() : true)
                .isActive(true) // Mặc định khi tạo mới
                .index(request.getIndex())
                .build();
    }

    // Đã xóa tham số EducationProgram program
    public void updateEntity(SubjectGroup group, SubjectGroupRequest request) {
        if (group == null || request == null) {
            return;
        }

        group.setName(request.getName());
        if (request.getIsGlobal() != null) {
            group.setIsGlobal(request.getIsGlobal());
        }
    }
}