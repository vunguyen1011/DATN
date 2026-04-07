package com.example.datn.Controller;

import com.example.datn.DTO.Request.ProgramSubjectRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.ProgramSubjectResponse;
import com.example.datn.Service.Interface.IProgramSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/program-subjects")
@RequiredArgsConstructor
public class ProgramSubjectController {

    private final IProgramSubjectService service;
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<ProgramSubjectResponse> create(@Valid @RequestBody ProgramSubjectRequest request) {
        return ApiResponse.<ProgramSubjectResponse>builder()
                .code(1000)
                .message("Thêm môn học vào chương trình đào tạo thành công")
                .result(service.create(request))
                .build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<ProgramSubjectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProgramSubjectRequest request) {
        return ApiResponse.<ProgramSubjectResponse>builder()
                .code(1000)
                .message("Cập nhật thông tin môn học thành công")
                .result(service.update(id, request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProgramSubjectResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<ProgramSubjectResponse>builder()
                .code(1000)
                .result(service.getById(id))
                .build();
    }

    @GetMapping("/section/{sectionId}")
    public ApiResponse<List<ProgramSubjectResponse>> getBySectionId(@PathVariable UUID sectionId) {
        return ApiResponse.<List<ProgramSubjectResponse>>builder()
                .code(1000)
                .result(service.getBySectionId(sectionId))
                .build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ApiResponse<Void> delete(
            @RequestParam UUID sectionId,
            @RequestParam UUID subjectId) {
        service.delete(sectionId, subjectId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa môn học khỏi chương trình đào tạo thành công")
                .build();
    }
}