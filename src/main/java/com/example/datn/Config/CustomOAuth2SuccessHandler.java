package com.example.datn.Config;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.TokenResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Service.Interface.IAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final IAuthService authService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String provider = oauthToken.getAuthorizedClientRegistrationId().toUpperCase();

            // Chỗ này có thể ném ra AppException (User not found)
            TokenResponse tokenResponse = authService.executeLogin(provider, authentication, response);

            String redirectUrl = frontendUrl + "?accessToken=" + tokenResponse.getAccessToken();
            response.sendRedirect(redirectUrl);

        } catch (AppException e) {
            // Bắt lỗi và tự tay format JSON trả về thay vì để Spring ném 500
            response.setStatus(e.getErrorCode().getCode());
            response.setContentType("application/json;charset=UTF-8");

            ApiResponse<Object> apiResponse = ApiResponse.builder()
                    .code(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();

            // Tự new ObjectMapper tại đây để tránh lỗi khởi tạo Bean sớm của Spring Security
            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(apiResponse));
        }
    }
}