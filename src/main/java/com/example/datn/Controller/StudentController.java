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
}
