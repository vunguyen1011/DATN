package com.example.datn.Controller;

import com.example.datn.DTO.Request.SectionDefaultSubjectRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SectionDefaultSubjectResponse;
import com.example.datn.Service.Interface.ISectionDefaultSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/section-default-subjects")
@RequiredArgsConstructor
public class SectionDefaultSubjectController {

    private final ISectionDefaultSubjectService service;

    @PostMapping
    public ApiResponse<SectionDefaultSubjectResponse> create(@Valid @RequestBody SectionDefaultSubjectRequest request) {
        return ApiResponse.<SectionDefaultSubjectResponse>builder()
                .code(1000)
                .message("Thêm môn học vào khối cam mẫu thành công")
                .result(service.create(request))
                .build();
    }

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

    @GetMapping("/section-default/{sectionDefaultId}")
    public ApiResponse<List<SectionDefaultSubjectResponse>> getBySectionDefaultId(@PathVariable UUID sectionDefaultId) {
        return ApiResponse.<List<SectionDefaultSubjectResponse>>builder()
                .code(1000)
                .result(service.getBySectionDefaultId(sectionDefaultId))
                .build();
    }
}