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

    @GetMapping("/program/{programId}")
    public ApiResponse<List<ProgramSubjectResponse>> getFlattenedByProgramId(@PathVariable UUID programId) {
        return ApiResponse.<List<ProgramSubjectResponse>>builder()
                .code(1000)
                .message("Lấy danh sách môn học phẳng trong CTĐT thành công")
                .result(service.getFlattenedByProgramId(programId))
                .build();
    }

    @GetMapping("/cohort/{cohortId}/major/{majorId}")
    public ApiResponse<org.springframework.data.domain.Page<ProgramSubjectResponse>> getSubjectsByCohortAndMajor(
            @PathVariable UUID cohortId, 
            @PathVariable UUID majorId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponse.<org.springframework.data.domain.Page<ProgramSubjectResponse>>builder()
                .code(1000)
                .message("Lấy danh sách môn học (phân trang) theo Khóa và Ngành thành công")
                .result(service.getSubjectsByCohortAndMajorPage(cohortId, majorId, search, pageable))
                .build();
    }

    @GetMapping("/cohort/{cohortId}/major/{majorId}/opened-this-semester")
    public ApiResponse<List<ProgramSubjectResponse>> getOpenedSubjectsForStudent(
            @PathVariable UUID cohortId, 
            @PathVariable UUID majorId) {
        return ApiResponse.<List<ProgramSubjectResponse>>builder()
                .code(1000)
                .message("Lấy mảng danh sách môn học Lọc theo tính trạng Đã Mở Kỳ Hiện Tại thành công")
                .result(service.getOpenedSubjectsForStudent(cohortId, majorId))
                .build();
    }

    @GetMapping("/cohort/{cohortId}/major/{majorId}/opened-this-semester/page")
    public ApiResponse<org.springframework.data.domain.Page<ProgramSubjectResponse>> getOpenedSubjectsForStudentPage(
            @PathVariable UUID cohortId, 
            @PathVariable UUID majorId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponse.<org.springframework.data.domain.Page<ProgramSubjectResponse>>builder()
                .code(1000)
                .message("Lấy danh sách môn học (Phân trang) Lọc theo tình trạng Đã Mở Kỳ Hiện Tại thành công")
                .result(service.getOpenedSubjectsForStudentPage(cohortId, majorId, search, pageable))
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