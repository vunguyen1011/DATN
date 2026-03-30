package com.example.datn.Service.Impl;

import com.example.datn.Service.Interface.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService implements IRedisService {

    // Sử dụng StringRedisTemplate vì cả key (username) và value (token) đều là chuỗi (String)
    private final StringRedisTemplate redisTemplate;

    // Tiền tố (prefix) để phân biệt các key của Refresh Token với các dữ liệu khác trong Redis
    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String OTP_PREFIX = "OTP:";

    @Override
    public void saveRefreshToken(String username, String refreshToken, Duration duration) {
        String key = buildKey(username);
        try {
            // Lưu token vào Redis và thiết lập thời gian hết hạn (TTL)
            redisTemplate.opsForValue().set(key, refreshToken, duration);
            log.info("Đã lưu Refresh Token vào Redis cho user: {}", username);
        } catch (Exception e) {
            log.error("Lỗi khi lưu Refresh Token vào Redis cho user: {}", username, e);
            // Tùy thuộc vào thiết kế, bạn có thể ném ra một Custom Exception ở đây
            // throw new AppException(ErrorCode.REDIS_CONNECTION_ERROR);
        }
    }

    @Override
    public String getRefreshToken(String username) {
        String key = buildKey(username);
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Lỗi khi lấy Refresh Token từ Redis cho user: {}", username, e);
            return null;
        }
    }

    @Override
    public void deleteRefreshToken(String username) {
        String key = buildKey(username);
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Đã xóa Refresh Token trong Redis của user: {}", username);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa Refresh Token trong Redis của user: {}", username, e);
        }
    }

    @Override
    public boolean isValidRefreshToken(String username, String refreshToken) {
        // Lấy token đang lưu trong Redis
        String storedToken = getRefreshToken(username);

        // Kiểm tra xem token có tồn tại và có khớp với token client gửi lên không
        if (storedToken == null) {
            log.warn("Không tìm thấy Refresh Token trong Redis cho user: {}", username);
            return false;
        }

        if (!storedToken.equals(refreshToken)) {
            log.warn("Refresh Token gửi lên không khớp với Redis cho user: {}", username);
            return false;
        }
        return true;
    }
    @Override
    public void saveOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        try {
            redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(5));
            log.info("Đã lưu OTP vào Redis cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi lưu OTP vào Redis cho email: {}", email, e);
        }
    }

    @Override
    public String getOtp(String email) {
        String key = OTP_PREFIX + email;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Lỗi khi lấy OTP từ Redis cho email: {}", email, e);
            return null;
        }
    }

    @Override
    public void deleteOtp(String email) {
        String key = OTP_PREFIX + email;
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Đã xóa OTP trong Redis của email: {}", email);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa OTP trong Redis của email: {}", email, e);
        }
    }

    private String buildKey(String username) {
        return REFRESH_TOKEN_PREFIX + username;
    }
}