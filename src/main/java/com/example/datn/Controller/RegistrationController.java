package com.example.datn.Controller;

import com.example.datn.DTO.Request.EnrollRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.EnrollmentResponse;
import com.example.datn.DTO.Response.RegistrationStatusResponse;
import com.example.datn.DTO.Response.SubjectResponse;
import com.example.datn.Service.Interface.IRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final IRegistrationService registrationService;

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/status")
    public ApiResponse<RegistrationStatusResponse> getRegistrationStatus() {
        return ApiResponse.<RegistrationStatusResponse>builder()
                .code(1000)
                .message("Lấy trạng thái đợt đăng ký thành công")
                .result(registrationService.getRegistrationStatus())
                .build();
    }



    @PreAuthorize("hasRole('USER')")
    @PostMapping("/enroll")
    public ApiResponse<EnrollmentResponse> enroll(@RequestBody EnrollRequest request) {
        return ApiResponse.<EnrollmentResponse>builder()
                .code(1000)
                .message("Đăng ký lớp học phần thành công")
                .result(registrationService.enroll(request))
                .build();
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/enroll/{classSectionId}")
    public ApiResponse<Void> cancelEnrollment(@PathVariable UUID classSectionId) {
        registrationService.cancelEnrollment(classSectionId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Hủy đăng ký lớp học phần thành công")
                .build();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-timetable")
    public ApiResponse<List<EnrollmentResponse>> getMyTimetable() {
        return ApiResponse.<List<EnrollmentResponse>>builder()
                .code(1000)
                .message("Lấy thời khóa biểu đăng ký thành công")
                .result(registrationService.getMyTimetable())
                .build();
    }
}
