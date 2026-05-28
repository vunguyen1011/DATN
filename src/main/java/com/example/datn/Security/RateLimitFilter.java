package com.example.datn.Security;

import com.example.datn.Service.Impl.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final StringRedisTemplate redisTemplate;

    private static final int GLOBAL_MAX_REQUESTS = 100;
    private static final int GLOBAL_WINDOW_SECONDS = 60;
    private static final int BAN_DURATION_SECONDS = 30;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/static") || uri.startsWith("/api/test")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier;
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            identifier = "user:" + auth.getName();
        } else {
            identifier = "ip:" + getClientIp(request);
        }

        String lockKey = "lock:ban:" + identifier;
        String bucketKey = "global_limit:" + identifier;

        String isBanned = redisTemplate.opsForValue().get(lockKey);
        if (isBanned != null) {
            handleErrorResponse(response, "Bạn đã bị tạm khóa 30 giây do gửi quá nhiều yêu cầu.");
            return;
        }

        RateLimitService.RateLimitResult result = rateLimitService.tryConsumeDetailed(bucketKey, GLOBAL_MAX_REQUESTS, GLOBAL_WINDOW_SECONDS);

        if (!result.isAllowed()) {
            redisTemplate.opsForValue().set(lockKey, "banned", BAN_DURATION_SECONDS, TimeUnit.SECONDS);

            log.warn("Client {} đã bị khóa cố định {} giây do vượt ngưỡng.", identifier, BAN_DURATION_SECONDS);
            handleErrorResponse(response, "Cảnh báo spam! Bạn bị khóa thao tác trong 30 giây.");
            return;
        }

        response.addHeader("X-Rate-Limit-Limit", String.valueOf(GLOBAL_MAX_REQUESTS));
        response.addHeader("X-Rate-Limit-Remaining", String.valueOf(result.remainingTokens()));
        filterChain.doFilter(request, response);
    }

    private void handleErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"code\": 429, \"message\": \"%s\"}", message));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader == null || xfHeader.isEmpty()) ? request.getRemoteAddr() : xfHeader.split(",")[0].trim();
    }
}