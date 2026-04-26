package com.example.datn.Controller;

import com.example.datn.DTO.Request.StudentGradeRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.StudentGradeResponse;
import com.example.datn.Service.Interface.IStudentGradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class StudentGradeController {

    private final IStudentGradeService studentGradeService;

    /**
     * Admin nhập / cập nhật điểm cho một enrollment.
     * POST /api/grades
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<StudentGradeResponse> upsertGrade(@Valid @RequestBody StudentGradeRequest request) {
        return ApiResponse.<StudentGradeResponse>builder()
                .code(1000)
                .message("Nhập / cập nhật điểm thành công")
                .result(studentGradeService.upsertGrade(request))
                .build();
    }

    /**
     * Sinh viên xem bảng điểm của chính mình.
     * GET /api/grades/my-transcript
     */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-transcript")
    public ApiResponse<List<StudentGradeResponse>> getMyTranscript() {
        return ApiResponse.<List<StudentGradeResponse>>builder()
                .code(1000)
                .message("Lấy bảng điểm thành công")
                .result(studentGradeService.getMyTranscript())
                .build();
    }
}
