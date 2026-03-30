package com.example.datn.Controller;

import com.example.datn.DTO.Request.AdminClassRequest;
import com.example.datn.DTO.Response.AdminClassResponse;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Service.Interface.IAdminClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin-classes")
@RequiredArgsConstructor
public class AdminClassController {

    private final IAdminClassService adminClassService;

    @PostMapping
    public ApiResponse<AdminClassResponse> createAdminClass(@Valid @RequestBody AdminClassRequest request) {
        return ApiResponse.<AdminClassResponse>builder()
                .code(1000)
                .message("Tạo lớp hành chính thành công")
                .result(adminClassService.createAdminClass(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<AdminClassResponse>> getAllAdminClasses(
            @RequestParam(value = "keyword", required = false) String keyword) {

        List<AdminClassResponse> resultList = (keyword != null && !keyword.trim().isEmpty())
                ? adminClassService.searchAdminClasses(keyword)
                : adminClassService.getAllAdminClasses();

        return ApiResponse.<List<AdminClassResponse>>builder()
                .code(1000)
                .message("Lấy danh sách lớp thành công")
                .result(resultList)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminClassResponse> getAdminClassById(@PathVariable UUID id) {
        return ApiResponse.<AdminClassResponse>builder()
                .code(1000)
                .message("Lấy thông tin lớp thành công")
                .result(adminClassService.getAdminClassById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminClassResponse> updateAdminClass(
            @PathVariable UUID id,
            @Valid @RequestBody AdminClassRequest request) {

        return ApiResponse.<AdminClassResponse>builder()
                .code(1000)
                .message("Cập nhật lớp thành công")
                .result(adminClassService.updateAdminClass(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAdminClass(@PathVariable UUID id) {
        adminClassService.deleteAdminClass(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa lớp thành công")
                .build();
    }
}