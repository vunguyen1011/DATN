package com.example.datn.Config;

import com.example.datn.Interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Áp dụng Interceptor cho toàn bộ các API bắt đầu bằng /api/
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}