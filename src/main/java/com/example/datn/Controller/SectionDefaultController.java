package com.example.datn.Controller;

import com.example.datn.DTO.Request.SectionDefaultRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SectionDefaultResponse;
import com.example.datn.Service.Interface.ISectionDefaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/section-defaults")
@RequiredArgsConstructor
public class SectionDefaultController {

    private final ISectionDefaultService sectionDefaultService;

    @PostMapping
    public ApiResponse<SectionDefaultResponse> create(@Valid @RequestBody SectionDefaultRequest request) {
        return ApiResponse.<SectionDefaultResponse>builder()
                .code(1000)
                .message("Tạo khối cam mẫu thành công")
                .result(sectionDefaultService.createSectionDefault(request))
                .build();
    }

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

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        sectionDefaultService.deleteSectionDefault(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa khối cam mẫu thành công")
                .build();
    }

    @GetMapping("/subject-group/{subjectGroupId}")
    public ApiResponse<List<SectionDefaultResponse>> getBySubjectGroup(@PathVariable UUID subjectGroupId) {
        return ApiResponse.<List<SectionDefaultResponse>>builder()
                .code(1000)
                .result(sectionDefaultService.getBySubjectGroupId(subjectGroupId))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SectionDefaultResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<SectionDefaultResponse>builder()
                .code(1000)
                .result(sectionDefaultService.getById(id))
                .build();
    }
}