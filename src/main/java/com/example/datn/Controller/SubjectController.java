package com.example.datn.Controller;

import com.example.datn.DTO.Request.PrerequisiteRequest;
import com.example.datn.DTO.Request.SubjectRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SubjectResponse;
import com.example.datn.Service.Interface.ISubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final ISubjectService subjectService;
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<SubjectResponse> createSubject(@RequestBody @Valid SubjectRequest request) {
        return ApiResponse.<SubjectResponse>builder()
                .result(subjectService.createSubject(request))
                .message("Tạo môn học thành công")
                .build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<SubjectResponse> updateSubject(
            @PathVariable UUID id,
            @RequestBody @Valid SubjectRequest request) {
        return ApiResponse.<SubjectResponse>builder()
                .result(subjectService.updateSubject(id, request))
                .message("Cập nhật môn học thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<List<SubjectResponse>> getAllSubjects(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.<List<SubjectResponse>>builder()
                .result(subjectService.getAllSubjects(keyword))
                .message("Lấy danh sách môn học thành công")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SubjectResponse> getSubjectById(@PathVariable UUID id) {
        return ApiResponse.<SubjectResponse>builder()
                .result(subjectService.getSubjectById(id))
                .message("Lấy thông tin môn học thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSubject(@PathVariable UUID id) {
        subjectService.deleteSubject(id);
        return ApiResponse.<Void>builder()
                .message("Xóa môn học thành công (Chuyển sang Inactive)")
                .build();
    }

    @GetMapping("/{id}/prerequisites")
    public ApiResponse<List<SubjectResponse>> getPrerequisites(@PathVariable UUID id) {
        return ApiResponse.<List<SubjectResponse>>builder()
                .result(subjectService.getPrerequisites(id))
                .message("Lấy danh sách điều kiện tiên quyết thành công")
                .build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/prerequisites")
    public ApiResponse<Void> updatePrerequisites(
            @PathVariable UUID id,
            @RequestBody @Valid PrerequisiteRequest request) {
        subjectService.updatePrerequisites(id, request.getPrerequisiteIds());
        return ApiResponse.<Void>builder()
                .message("Cập nhật điều kiện tiên quyết thành công")
                .build();
    }
    // 1. Xem môn học này đang là tiên quyết cho những môn nào (Ai cần môn này?)


//    @GetMapping("/{id}/prerequisites")
//    public ApiResponse<List<SubjectResponse>> getPrerequisiteTree(@PathVariable UUID id) {
//        return ApiResponse.<List<SubjectResponse>>builder()
//                .code(1000)
//                .message("Lấy cây điều kiện tiên quyết thành công")
//                .result(subjectService.getPrerequisiteTree(id))
//                .build();
//    }


}