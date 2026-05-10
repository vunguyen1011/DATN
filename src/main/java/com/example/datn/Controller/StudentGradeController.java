package com.example.datn.Controller;

import com.example.datn.DTO.Request.FinalGradeRequest;
import com.example.datn.DTO.Request.MidtermGradeRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.StudentGradeResponse;
import com.example.datn.Service.Interface.IStudentGradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class StudentGradeController {

    private final IStudentGradeService studentGradeService;

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @PatchMapping("/midterm/{enrollmentId}")
    public ApiResponse<StudentGradeResponse> updateMidtermScore(
            @PathVariable UUID enrollmentId,
            @RequestParam Double score) {
        return ApiResponse.<StudentGradeResponse>builder()
                .code(1000)
                .message("Cập nhật điểm giữa kỳ thành công")
                .result(studentGradeService.updateMidtermScore(enrollmentId, score))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @PatchMapping("/final/{enrollmentId}")
    public ApiResponse<StudentGradeResponse> updateFinalScore(
            @PathVariable UUID enrollmentId,
            @RequestParam Double score) {
        return ApiResponse.<StudentGradeResponse>builder()
                .code(1000)
                .message("Cập nhật điểm cuối kỳ thành công")
                .result(studentGradeService.updateFinalScore(enrollmentId, score))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @PutMapping("/class-section/{classSectionId}/midterm")
    public ApiResponse<Void> updateClassSectionMidtermGrades(
            @PathVariable UUID classSectionId,
            @Valid @RequestBody List<MidtermGradeRequest> requests) {
        studentGradeService.updateClassSectionMidtermGrades(classSectionId, requests);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Cập nhật điểm giữa kỳ cho lớp học phần thành công")
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @PutMapping("/class-section/{classSectionId}/final")
    public ApiResponse<Void> updateClassSectionFinalGrades(
            @PathVariable UUID classSectionId,
            @Valid @RequestBody List<FinalGradeRequest> requests) {
        studentGradeService.updateClassSectionFinalGrades(classSectionId, requests);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Cập nhật điểm cuối kỳ cho lớp học phần thành công")
                .build();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-transcript")
    public ApiResponse<List<StudentGradeResponse>> getMyTranscript() {
        return ApiResponse.<List<StudentGradeResponse>>builder()
                .code(1000)
                .message("Lấy bảng điểm thành công")
                .result(studentGradeService.getMyTranscript())
                .build();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-transcript-tree")
    public ApiResponse<com.example.datn.DTO.Response.TranscriptTreeResponse> getMyTranscriptTree() {
        return ApiResponse.<com.example.datn.DTO.Response.TranscriptTreeResponse>builder()
                .code(1000)
                .message("Lấy bảng điểm dạng cây thành công")
                .result(studentGradeService.getMyTranscriptTree())
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @GetMapping("/class-section/{classSectionId}")
    public ApiResponse<List<com.example.datn.DTO.Response.ClassSectionStudentGradeResponse>> getStudentsGradesByClassSection(
            @PathVariable UUID classSectionId) {
        return ApiResponse.<List<com.example.datn.DTO.Response.ClassSectionStudentGradeResponse>>builder()
                .code(1000)
                .message("Lấy danh sách điểm của lớp học phần thành công")
                .result(studentGradeService.getStudentsGradesByClassSection(classSectionId))
                .build();
    }
}