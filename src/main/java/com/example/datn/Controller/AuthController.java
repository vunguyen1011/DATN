package com.example.datn.Controller;

import com.example.datn.Annotation.RateLimit;
import com.example.datn.DTO.Request.*;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.TokenResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Service.Interface.IAuthService;
import com.example.datn.Service.Interface.IExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Authentication", description = "Quản lý xác thực, đăng nhập, cấp lại mật khẩu và import dữ liệu Excel")
@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class AuthController {

    private final IExcelService excelService;
    private final IAuthService authService;

    @Operation(summary = "Đăng nhập hệ thống")
    @RateLimit(requests = 5, window = 30)
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.executeLogin("LOCAL", request, response);
        return ApiResponse.<TokenResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công")
                .result(tokenResponse)
                .build();
    }

    @Operation(summary = "Làm mới Access Token từ Refresh Token")
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

    @Operation(summary = "Nhập danh sách sinh viên từ file Excel")
    @RateLimit(requests = 5, window = 30)
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

    @Operation(summary = "Tải file mẫu Excel nhập sinh viên")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/excel-template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=template_sinh_vien.xlsx");
        excelService.downloadTemplate(response);
    }

    @Operation(summary = "Tải file mẫu Excel nhập giảng viên")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/excel-template-lecturer")
    public void downloadTemplateLecturer(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=template_giang_vien.xlsx");
        excelService.downloadTemplateLecturer(response);
    }

    @Operation(summary = "Đăng xuất khỏi hệ thống")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.logout(username, response);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đăng xuất thành công")
                .build();
    }

    @Operation(summary = "Thay đổi mật khẩu người dùng")
    @RateLimit(requests = 5, window = 30)
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(username, request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đổi mật khẩu thành công")
                .build();
    }

    @Operation(summary = "Yêu cầu khôi phục mật khẩu (Gửi OTP)")
    @RateLimit(requests = 5, window = 30)
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Mã OTP đã được gửi đến email của bạn")
                .build();
    }

    @Operation(summary = "Xác thực mã OTP khôi phục mật khẩu")
    @RateLimit(requests = 5, window = 30)
    @PostMapping("/verify-otp")
    public ApiResponse<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String resetToken = authService.verifyOtp(request);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xác thực OTP thành công")
                .result(resetToken)
                .build();
    }

    @Operation(summary = "Đặt lại mật khẩu mới")
    @RateLimit(requests = 5, window = 10)
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Khôi phục mật khẩu thành công")
                .build();
    }

    @Operation(summary = "Nhập danh sách giảng viên từ file Excel")
    @RateLimit(requests = 3, window = 30)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import-lecturers-excel")
    public ApiResponse<String> importLecturersFromExcel(@RequestParam("file") MultipartFile file) {
        String resultMessage = excelService.saveLecturersFromExcel(file);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xử lý file Excel giảng viên hoàn tất!")
                .result(resultMessage)
                .build();
    }

    @Operation(summary = "Bổ nhiệm vai trò Trưởng phòng/Quản trị viên")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign")
    public ApiResponse<Void> assignRoleToUser(@RequestParam String username) {
        authService.assignRoleToUser(username);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Bổ nhiệm Trưởng phòng thành công")
                .build();
    }
}