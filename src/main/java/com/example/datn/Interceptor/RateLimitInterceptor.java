package com.example.datn.Interceptor;

import com.example.datn.Annotation.RateLimit; // Hãy đảm bảo bạn đã tạo file Annotation này
import com.example.datn.Security.MyUserDetail;
import com.example.datn.Service.Impl.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. Chỉ kiểm tra nếu request đang trỏ tới một hàm trong Controller
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 2. Tìm xem hàm (API) này có được gắn nhãn @RateLimit không
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true; // Nếu không gắn nhãn -> Không giới hạn (Cho qua luôn)
        }

        // 3. Lấy định danh người dùng (Ưu tiên StudentId, fallback về Username hoặc IP)
        String identifier = getIdentifier(request);
        String uri = request.getRequestURI();

        // 4. Tạo Key cho xô Token riêng biệt: "rate_limit_api:/api/enroll:20221234"
        String key = "rate_limit_api:" + uri + ":" + identifier;

        // 5. Kiểm tra Rate Limit qua Bucket4j (Lấy thông số động từ Annotation)
        RateLimitService.RateLimitResult result = rateLimitService.tryConsumeDetailed(
                key, rateLimit.requests(), rateLimit.window());

        if (!result.isAllowed()) {
            long waitSeconds = result.nanosToWaitForRefill() / 1_000_000_000;
            log.warn("User/IP {} bị chặn tại API {} do spam. Chờ {}s", identifier, uri, waitSeconds);

            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                    "{\"code\": 429, \"message\": \"Thao tác quá nhanh! Vui lòng đợi %d giây nữa.\"}",
                    waitSeconds));
            return false; // Ngắt luồng tại đây, không cho vào Controller
        }

        return true;
    }

    /**
     * Hàm phụ trợ: Lấy định danh người dùng một cách chính xác
     */
    private String getIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Nếu đã đăng nhập
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            // Ép kiểu để lấy StudentId
            if (authentication.getPrincipal() instanceof MyUserDetail userDetail) {
                if (userDetail.getStudentId() != null) {
                    return userDetail.getStudentId().toString();
                }
            }
            // Nếu không có StudentId thì dùng Username
            return authentication.getName();
        }

        // Nếu chưa đăng nhập (VD: Các API public bị gắn @RateLimit), lấy IP
        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader == null || xfHeader.isEmpty()) ? request.getRemoteAddr() : xfHeader.split(",")[0].trim();
    }
}