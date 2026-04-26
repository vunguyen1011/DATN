package com.example.datn.Controller;

import com.example.datn.DTO.Request.RegistrationPeriodRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.RegistrationPeriodResponse;
import com.example.datn.Service.Interface.IRegistrationPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/registration-periods")
@RequiredArgsConstructor
public class RegistrationPeriodController {

    private final IRegistrationPeriodService registrationPeriodService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RegistrationPeriodResponse> createRegistrationPeriod(
            @Valid @RequestBody RegistrationPeriodRequest request) {
        return ApiResponse.<RegistrationPeriodResponse>builder()
                .code(1000)
                .message("Tạo đợt đăng ký thành công")
                .result(registrationPeriodService.createRegistrationPeriod(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RegistrationPeriodResponse> updateRegistrationPeriod(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrationPeriodRequest request) {
        return ApiResponse.<RegistrationPeriodResponse>builder()
                .code(1000)
                .message("Cập nhật đợt đăng ký thành công")
                .result(registrationPeriodService.updateRegistrationPeriod(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteRegistrationPeriod(@PathVariable UUID id) {
        registrationPeriodService.deleteRegistrationPeriod(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa đợt đăng ký thành công")
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'USER')")
    public ApiResponse<RegistrationPeriodResponse> getRegistrationPeriodById(@PathVariable UUID id) {
        return ApiResponse.<RegistrationPeriodResponse>builder()
                .code(1000)
                .message("Lấy thông tin đợt đăng ký thành công")
                .result(registrationPeriodService.getRegistrationPeriodById(id))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<RegistrationPeriodResponse>> getAllRegistrationPeriods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<RegistrationPeriodResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đợt đăng ký thành công")
                .result(registrationPeriodService.getAllRegistrationPeriods(pageable))
                .build();
    }

    @GetMapping("/semester/{semesterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'USER')")
    public ApiResponse<Page<RegistrationPeriodResponse>> getRegistrationPeriodsBySemester(
            @PathVariable UUID semesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<RegistrationPeriodResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đợt đăng ký theo học kỳ thành công")
                .result(registrationPeriodService.getRegistrationPeriodsBySemester(semesterId, pageable))
                .build();
    }
}
