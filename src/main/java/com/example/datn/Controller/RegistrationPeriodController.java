        package com.example.datn.Controller;

import com.example.datn.DTO.Request.RegistrationPeriodRequest;
import com.example.datn.DTO.Request.RegistrationPeriodUpdateRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.RegistrationPeriodResponse;
import com.example.datn.Service.Interface.IRegistrationPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Registration Period", description = "Quản lý các đợt đăng ký tín chỉ")
@RestController
@RequestMapping("/api/registration-periods")
@RequiredArgsConstructor
public class RegistrationPeriodController {

    private final IRegistrationPeriodService registrationPeriodService;

    @Operation(summary = "Tạo đợt đăng ký mới")
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

    @Operation(summary = "Cập nhật thông tin đợt đăng ký")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RegistrationPeriodResponse> updateRegistrationPeriod(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrationPeriodUpdateRequest request) {
        return ApiResponse.<RegistrationPeriodResponse>builder()
                .code(1000)
                .message("Cập nhật đợt đăng ký thành công")
                .result(registrationPeriodService.updateRegistrationPeriod(id, request))
                .build();
    }

    @Operation(summary = "Xóa đợt đăng ký")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteRegistrationPeriod(@PathVariable UUID id) {
        registrationPeriodService.deleteRegistrationPeriod(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa đợt đăng ký thành công")
                .build();
    }

    @Operation(summary = "Lấy chi tiết đợt đăng ký theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'USER')")
    public ApiResponse<RegistrationPeriodResponse> getRegistrationPeriodById(@PathVariable UUID id) {
        return ApiResponse.<RegistrationPeriodResponse>builder()
                .code(1000)
                .message("Lấy thông tin đợt đăng ký thành công")
                .result(registrationPeriodService.getRegistrationPeriodById(id))
                .build();
    }

    @Operation(summary = "Lấy toàn bộ danh sách đợt đăng ký (phân trang)")
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

    @Operation(summary = "Lấy danh sách đợt đăng ký theo học kỳ (phân trang)")
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
