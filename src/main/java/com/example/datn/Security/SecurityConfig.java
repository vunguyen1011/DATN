package com.example.datn.Security;

import com.example.datn.Config.CustomAuthEntryPoint;
import com.example.datn.Config.CustomOAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // Cho phép dùng @PreAuthorize("hasRole('ADMIN')") ở Controller
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomAuthEntryPoint customAuthEntryPoint;

    // Danh sách các URL không cần check Token
    private final String[] WHITE_LIST = {
            "/api/auth/login",          // API đăng nhập nội bộ
            "/api/auth/refresh-token",  // API cấp lại Access Token
            "/oauth2/**",               // Luồng OAuth2 mặc định của Spring
            "/login/oauth2/**",         // Callback từ Google/Teams
            "/v3/api-docs/**",          // Swagger docs
            "/swagger-ui/**",           // Swagger UI
            "/swagger-ui.html",
            "/api/test/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable) // Tắt CSRF vì dùng JWT

                // Cấu hình Stateless (không lưu Session trên Server)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Phân quyền request
                .authorizeHttpRequests(request -> request
                        .requestMatchers(WHITE_LIST).permitAll() // Cho phép White List
                        .anyRequest().authenticated()          // Tất cả các API còn lại phải có Token
                )

                // Cấu hình đăng nhập bằng Google/Teams
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customOAuth2SuccessHandler) // Gọi Handler điều hướng về FE
                )

                // Gắn bộ lọc kiểm tra JWT vào trước bộ lọc xác thực mặc định
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Xử lý lỗi khi Token sai hoặc không có quyền truy cập
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(customAuthEntryPoint)
                );

        return httpSecurity.build();
    }
}