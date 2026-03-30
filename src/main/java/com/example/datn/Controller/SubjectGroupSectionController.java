package com.example.datn.Controller;

import com.example.datn.DTO.Request.SubjectGroupSectionRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SubjectGroupSectionResponse;
import com.example.datn.Service.Interface.ISubjectGroupSectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subject-group-sections")
@RequiredArgsConstructor
public class SubjectGroupSectionController {

    private final ISubjectGroupSectionService sectionService;

    @PostMapping
    public ApiResponse<SubjectGroupSectionResponse> createSection(@Valid @RequestBody SubjectGroupSectionRequest request) {
        return ApiResponse.<SubjectGroupSectionResponse>builder()
                .code(1000)
                .message("Tạo đoạn môn học thành công")
                .result(sectionService.createSection(request))
                .build();
    }

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

    @GetMapping("/{id}")
    public ApiResponse<SubjectGroupSectionResponse> getSectionById(@PathVariable UUID id) {
        return ApiResponse.<SubjectGroupSectionResponse>builder()
                .code(1000)
                .result(sectionService.getSectionById(id))
                .build();
    }


    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSection(@PathVariable UUID id) {
        sectionService.softDeleteSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa đoạn môn học thành công")
                .build();
    }
}