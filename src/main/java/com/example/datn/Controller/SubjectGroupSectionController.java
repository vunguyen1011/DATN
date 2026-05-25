package com.example.datn.Controller;

import com.example.datn.DTO.Request.SubjectGroupSectionRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SubjectGroupSectionResponse;
import com.example.datn.Service.Interface.ISubjectGroupSectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Subject Group Section", description = "Quản lý phần/mục nhóm môn học trong cấu trúc chương trình đào tạo")
@RestController
@RequestMapping("/api/subject-group-sections")
@RequiredArgsConstructor
public class SubjectGroupSectionController {

    private final ISubjectGroupSectionService sectionService;

    @Operation(summary = "Tạo phần/mục nhóm môn học mới")
    @PostMapping
    public ApiResponse<SubjectGroupSectionResponse> createSection(@Valid @RequestBody SubjectGroupSectionRequest request) {
        return ApiResponse.<SubjectGroupSectionResponse>builder()
                .code(1000)
                .message("Tạo đoạn môn học thành công")
                .result(sectionService.createSection(request))
                .build();
    }

    @Operation(summary = "Cập nhật thông tin phần/mục nhóm môn học")
    @PutMapping("/{id}")
    public ApiResponse<SubjectGroupSectionResponse> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectGroupSectionRequest request) {
        return ApiResponse.<SubjectGroupSectionResponse>builder()
                .code(1000)
                .message("Cập nhật đoạn môn học thành công")
                .result(sectionService.updateSection(id, request))
                .build();
    }

    @Operation(summary = "Lấy chi tiết phần/mục nhóm môn học theo ID")
    @GetMapping("/{id}")
    public ApiResponse<SubjectGroupSectionResponse> getSectionById(@PathVariable UUID id) {
        return ApiResponse.<SubjectGroupSectionResponse>builder()
                .code(1000)
                .result(sectionService.getSectionById(id))
                .build();
    }


    @Operation(summary = "Xóa phần/mục nhóm môn học")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSection(@PathVariable UUID id) {
        sectionService.softDeleteSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa đoạn môn học thành công")
                .build();
    }
}