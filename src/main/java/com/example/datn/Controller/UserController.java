package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Service.Interface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}