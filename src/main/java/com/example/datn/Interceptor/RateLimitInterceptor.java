package com.example.datn.Interceptor;

import com.example.datn.Security.MyUserDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_REQUESTS_PER_SECOND = 5;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true; // Bỏ qua nếu chưa auth (để Security Filter lo)
        }

        String studentId = null;
        if (authentication.getPrincipal() instanceof MyUserDetail userDetail) {
            if (userDetail.getStudentId() != null) {
                studentId = userDetail.getStudentId().toString();
            }
        }

        if (studentId == null) {
            studentId = authentication.getName(); // Fallback dùng username
        }

        long currentSecond = Instant.now().getEpochSecond();
        String key = "rate_limit:" + studentId + ":" + currentSecond;

        Long requests = redisTemplate.opsForValue().increment(key);
        if (requests != null && requests == 1) {
            redisTemplate.expire(key, 2, TimeUnit.SECONDS); // Expire sau 2 giây để dọn rác
        }

        if (requests != null && requests > MAX_REQUESTS_PER_SECOND) {
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\": 429, \"message\": \"Too Many Requests. Vui lòng thao tác chậm lại.\"}");
            return false;
        }

        return true;
    }
}
