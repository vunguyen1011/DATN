package com.example.datn.Controller;

import com.example.datn.DTO.Request.ChangePasswordRequest;
import com.example.datn.DTO.Request.ForgotPasswordRequest;
import com.example.datn.DTO.Request.LoginRequest;
import com.example.datn.DTO.Request.ResetPasswordRequest;
import com.example.datn.DTO.Request.VerifyOtpRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.TokenResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Service.Interface.IAuthService;
import com.example.datn.Service.Interface.IExcelService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class AuthController {

    private final IExcelService excelService;
    private final IAuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.executeLogin("LOCAL", request, response);
        return ApiResponse.<TokenResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công")
                .result(tokenResponse)
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<TokenResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        TokenResponse tokenResponse = authService.refreshToken(refreshToken, response);

        return ApiResponse.<TokenResponse>builder()
                .code(1000)
                .message("Refresh Token thành công")
                .result(tokenResponse)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import-excel")
    public ApiResponse<String> importStudentsFromExcel(@RequestParam("file") MultipartFile file) {
        String resultMessage = excelService.saveUsersFromExcel(file);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xử lý file Excel hoàn tất!")
                .result(resultMessage)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/excel-template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=template_sinh_vien.xlsx");
        excelService.downloadTemplate(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.logout(username, response);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đăng xuất thành công")
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(username, request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đổi mật khẩu thành công")
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Mã OTP đã được gửi đến email của bạn")
                .build();
    }

    @PostMapping("/verify-otp")
    public ApiResponse<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String resetToken = authService.verifyOtp(request);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xác thực OTP thành công")
                .result(resetToken)
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Khôi phục mật khẩu thành công")
                .build();
    }
}