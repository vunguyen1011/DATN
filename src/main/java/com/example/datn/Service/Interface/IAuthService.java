package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.*;
import com.example.datn.DTO.Response.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthService {
    TokenResponse refreshToken(String refreshToken, HttpServletResponse response);
    <T> TokenResponse executeLogin(String loginMethod, T requestData, HttpServletResponse response);
    void logout(String username, HttpServletResponse response);
    void changePassword(String username, ChangePasswordRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    String verifyOtp(VerifyOtpRequest request);
    void resetPassword(ResetPasswordRequest request);
    void assignRoleToUser(AssignRoleRequest request);
}