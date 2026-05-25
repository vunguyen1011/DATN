package com.example.datn.Controller;

import com.example.datn.DTO.Request.SectionDefaultRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SectionDefaultResponse;
import com.example.datn.Service.Interface.ISectionDefaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Section Default", description = "Quản lý lớp hành chính mặc định (Khối cam mẫu)")
@RestController
@RequestMapping("/api/section-defaults")
@RequiredArgsConstructor
public class SectionDefaultController {

    private final ISectionDefaultService sectionDefaultService;

    @Operation(summary = "Tạo lớp hành chính mặc định mới")
    @PostMapping
    public ApiResponse<SectionDefaultResponse> create(@Valid @RequestBody SectionDefaultRequest request) {
        return ApiResponse.<SectionDefaultResponse>builder()
                .code(1000)
                .message("Tạo khối cam mẫu thành công")
                .result(sectionDefaultService.createSectionDefault(request))
                .build();
    }

    @Operation(summary = "Cập nhật thông tin lớp hành chính mặc định")
    @PutMapping("/{id}")
    public ApiResponse<SectionDefaultResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SectionDefaultRequest request) {
        return ApiResponse.<SectionDefaultResponse>builder()
                .code(1000)
                .message("Cập nhật khối cam mẫu thành công")
                .result(sectionDefaultService.updateSectionDefault(id, request))
                .build();
    }

    @Operation(summary = "Xóa lớp hành chính mặc định")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        sectionDefaultService.deleteSectionDefault(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa khối cam mẫu thành công")
                .build();
    }

    @Operation(summary = "Lấy danh sách lớp hành chính mặc định theo nhóm môn học")
    @GetMapping("/subject-group/{subjectGroupId}")
    public ApiResponse<List<SectionDefaultResponse>> getBySubjectGroup(@PathVariable UUID subjectGroupId) {
        return ApiResponse.<List<SectionDefaultResponse>>builder()
                .code(1000)
                .result(sectionDefaultService.getBySubjectGroupId(subjectGroupId))
                .build();
    }

    @Operation(summary = "Lấy chi tiết lớp hành chính mặc định theo ID")
    @GetMapping("/{id}")
    public ApiResponse<SectionDefaultResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<SectionDefaultResponse>builder()
                .code(1000)
                .result(sectionDefaultService.getById(id))
                .build();
    }
}