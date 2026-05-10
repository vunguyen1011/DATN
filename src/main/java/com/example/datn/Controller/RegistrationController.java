package com.example.datn.Controller;

import com.example.datn.DTO.Request.EnrollRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.EnrollmentResponse;
import com.example.datn.DTO.Response.EnrollmentSimpleResponse;
import com.example.datn.DTO.Response.RegistrationStatusResponse;
import com.example.datn.DTO.Response.SubjectResponse;
import com.example.datn.Model.Enrollment;
import com.example.datn.Service.Interface.IRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;

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
    @SentinelResource(value = "enrollment_api", blockHandler = "handleEnrollmentBlock")
    public ApiResponse<List<EnrollmentSimpleResponse>> enroll(@RequestBody EnrollRequest request) {
        return ApiResponse.<List<EnrollmentSimpleResponse>>builder()
                .code(1000)
                .message("Đăng ký lớp học phần thành công")
                .result(registrationService.enroll(request))
                .build();
    }

    // Fallback method khi lượng truy cập vượt ngưỡng Sentinel (1000 QPS)
    public ApiResponse<List<EnrollmentSimpleResponse>> handleEnrollmentBlock(@RequestBody EnrollRequest request, BlockException ex) {
        return ApiResponse.<List<EnrollmentSimpleResponse>>builder()
                .code(429)
                .message("Hệ thống đang quá tải do lượng sinh viên truy cập quá lớn. Vui lòng thử lại sau ít phút!")
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
    @GetMapping("/in-class-section/{classSectionId}")
    public Page<EnrollmentResponse> findEnrollmentsByClassSectionId(
            @PathVariable UUID classSectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return registrationService.getAllEnrollmentInClassSection(classSectionId,pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/clear-redis")
    public ApiResponse<Void> clearRedisDataAfterRegistration() {
        registrationService.clearRedisDataAfterRegistration();
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã dọn dẹp toàn bộ dữ liệu đăng ký tín chỉ trên Redis thành công")
                .build();
    }
}
