package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Service.Interface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "User Management", description = "Quản lý thông tin người dùng và tài khoản")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @Operation(summary = "Lấy thông tin tài khoản đang đăng nhập")
    @GetMapping("/my-info")
    public ApiResponse<UserProfileResponse> getMyInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileResponse response = userService.getMyInfo(username);

        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .message("Lấy thông tin thành công")
                .result(response)
                .build();
    }


    @Operation(summary = "Lấy thông tin người dùng theo tên tài khoản (username)")
    @GetMapping("/{username}")
    public ApiResponse<UserProfileResponse> getUserInfo(@PathVariable String username) {
        UserProfileResponse response = userService.getMyInfo(username);

        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .message("Lấy thông tin thành công")
                .result(response)
                .build();
    }

    @Operation(summary = "Lấy danh sách tất cả người dùng trong hệ thống (phân trang & tìm kiếm)")
    @GetMapping
    public ApiResponse<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfileResponse> response = userService.getAllUsers(search, pageable);
        return ApiResponse.<Page<UserProfileResponse>>builder()
                .code(1000)
                .message("Lấy danh sách người dùng thành công")
                .result(response)
                .build();
    }

    @Operation(summary = "Lấy chi tiết người dùng theo ID")
    @GetMapping("/id/{id}")
    public ApiResponse<UserProfileResponse> getUserById(@PathVariable UUID id) {
        UserProfileResponse response = userService.getUserById(id);
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .message("Lấy thông tin người dùng thành công")
                .result(response)
                .build();
    }

    @Operation(summary = "Bật/Tắt hoạt động của tài khoản người dùng")
    @PutMapping("/{id}/toggle-status")
    public ApiResponse<UserProfileResponse> toggleUserStatus(@PathVariable UUID id) {
        UserProfileResponse response = userService.toggleUserStatus(id);
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .message("Thay đổi trạng thái người dùng thành công")
                .result(response)
                .build();
    }
    @Operation(summary = "Lấy danh sách người dùng theo vai trò (Role)")
    @GetMapping("/get-by-role")
    public ApiResponse<Page<UserProfileResponse>> getUsersByRole(
            @RequestParam String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfileResponse> response = userService.getAllUsersByRole(role, pageable);
        return ApiResponse.<Page<UserProfileResponse>>builder()
                .code(1000)
                .message("Lấy danh sách người dùng theo vai trò thành công")
                .result(response)
                .build();
    }
    @Operation(summary = "Lấy danh sách giảng viên thuộc ngành học tương ứng")
    @GetMapping("/lecturers/{majorId}")
    public ApiResponse<Page<UserProfileResponse>> getAllLecturers(@PathVariable UUID majorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfileResponse> response = userService.getLecturersByMajorId(majorId, pageable);
        return ApiResponse.<Page<UserProfileResponse>>builder()
                .code(1000)
                .message("Lấy danh sách giảng viên thành công")
                .result(response)
                .build();
        }
}