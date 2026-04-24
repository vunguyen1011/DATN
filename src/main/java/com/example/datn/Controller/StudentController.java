package com.example.datn.Controller;

import com.example.datn.DTO.Request.StudentUpdateRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Service.Interface.IStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final IStudentService studentService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/export-pdf")
    public void exportStudentsToPdf(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        studentService.exportStudentsToPdf(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/profile")
    public ApiResponse<UserProfileResponse.StudentProfile> updateStudentProfile(
            @PathVariable UUID id,
            @RequestBody StudentUpdateRequest request) {
        UserProfileResponse.StudentProfile response = studentService.updateStudentProfile(id, request);
        return ApiResponse.<UserProfileResponse.StudentProfile>builder()
                .code(1000)
                .message("Cập nhật thông tin sinh viên thành công")
                .result(response)
                .build();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-program")
    public ApiResponse<java.util.List<com.example.datn.DTO.Response.ProgramSubjectResponse>> getMyProgram() {
        return ApiResponse.<java.util.List<com.example.datn.DTO.Response.ProgramSubjectResponse>>builder()
                .code(1000)
                .message("Lấy chương trình đào tạo thành công")
                .result(studentService.getMyProgram())
                .build();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-program-tree")
    public ApiResponse<com.example.datn.DTO.Response.ProgramTreeResponse> getMyProgramTree() {
        return ApiResponse.<com.example.datn.DTO.Response.ProgramTreeResponse>builder()
                .code(1000)
                .message("Lấy chương trình đào tạo (dạng cây) thành công")
                .result(studentService.getMyProgramTree())
                .build();
    }
}
