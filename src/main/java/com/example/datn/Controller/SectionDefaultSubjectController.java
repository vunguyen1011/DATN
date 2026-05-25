package com.example.datn.Controller;

import com.example.datn.DTO.Request.SectionDefaultSubjectRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SectionDefaultSubjectResponse;
import com.example.datn.DTO.Response.TemplateTreeResponse;
import com.example.datn.Service.Interface.ISectionDefaultSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Section Default Subject", description = "Quản lý môn học mặc định theo lớp hành chính mặc định")
@RestController
@RequestMapping("/api/section-default-subjects")
@RequiredArgsConstructor
public class SectionDefaultSubjectController {

    private final ISectionDefaultSubjectService service;

    @Operation(summary = "Thêm môn học mặc định vào lớp hành chính mặc định")
    @PostMapping
    public ApiResponse<SectionDefaultSubjectResponse> create(@Valid @RequestBody SectionDefaultSubjectRequest request) {
        return ApiResponse.<SectionDefaultSubjectResponse>builder()
                .code(1000)
                .message("Thêm môn học vào khối cam mẫu thành công")
                .result(service.create(request))
                .build();
    }

    @Operation(summary = "Cập nhật môn học mặc định của lớp hành chính mặc định")
    @PutMapping("/{id}")
    public ApiResponse<SectionDefaultSubjectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SectionDefaultSubjectRequest request) {
        return ApiResponse.<SectionDefaultSubjectResponse>builder()
                .code(1000)
                .message("Cập nhật môn học trong khối cam mẫu thành công")
                .result(service.update(id, request))
                .build();
    }

    @Operation(summary = "Xóa môn học mặc định khỏi lớp hành chính mặc định")
    @DeleteMapping
    public ApiResponse<Void> delete(
            @RequestParam UUID sectionDefaultId,
            @RequestParam UUID subjectId) {
        service.delete(sectionDefaultId, subjectId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa môn học khỏi khối cam mẫu thành công")
                .build();
    }

    @Operation(summary = "Lấy danh sách môn học mặc định theo ID lớp hành chính mặc định")
    @GetMapping("/section-default/{sectionDefaultId}")
    public ApiResponse<List<SectionDefaultSubjectResponse>> getBySectionDefaultId(@PathVariable UUID sectionDefaultId) {
        return ApiResponse.<List<SectionDefaultSubjectResponse>>builder()
                .code(1000)
                .result(service.getBySectionDefaultId(sectionDefaultId))
                .build();
    }
    @Operation(summary = "Lấy cây cấu trúc chương trình đào tạo mẫu")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/template-tree")
    public ApiResponse<TemplateTreeResponse> getTemplateTree() {
        return ApiResponse.<TemplateTreeResponse>builder()
                .code(1000)
                .message("Lấy cây chương trình mẫu thành công")
                .result(service.getTemplateTree())
                .build();
    }
}