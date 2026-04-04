package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Service.Interface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

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


    @GetMapping("/{username}")
    public ApiResponse<UserProfileResponse> getUserInfo(@PathVariable String username) {
        UserProfileResponse response = userService.getMyInfo(username);

        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .message("Lấy thông tin thành công")
                .result(response)
                .build();
    }

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

    @GetMapping("/id/{id}")
    public ApiResponse<UserProfileResponse> getUserById(@PathVariable UUID id) {
        UserProfileResponse response = userService.getUserById(id);
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .message("Lấy thông tin người dùng thành công")
                .result(response)
                .build();
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<UserProfileResponse> toggleUserStatus(@PathVariable UUID id) {
        UserProfileResponse response = userService.toggleUserStatus(id);
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .message("Thay đổi trạng thái người dùng thành công")
                .result(response)
                .build();
    }
}