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

    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String OTP_PREFIX = "OTP:";
    private static final String RESET_TOKEN_PREFIX = "RESET_TOKEN:";

    @Override
    public void saveRefreshToken(String username, String refreshToken, Duration duration) {
        String key = buildKey(username);
        try {
            redisTemplate.opsForValue().set(key, refreshToken, duration);
            log.info("Đã lưu Refresh Token vào Redis cho user: {}", username);
        } catch (Exception e) {
            log.error("Lỗi khi lưu Refresh Token vào Redis cho user: {}", username, e);
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
        String storedToken = getRefreshToken(username);

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

    @Override
    public void saveResetToken(String email, String resetToken, Duration duration) {
        String key = RESET_TOKEN_PREFIX + email;
        try {
            redisTemplate.opsForValue().set(key, resetToken, duration);
            log.info("Đã lưu Reset Token vào Redis cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi lưu Reset Token vào Redis cho email: {}", email, e);
        }
    }

    @Override
    public String getResetToken(String email) {
        String key = RESET_TOKEN_PREFIX + email;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Lỗi khi lấy Reset Token từ Redis cho email: {}", email, e);
            return null;
        }
    }

    @Override
    public void deleteResetToken(String email) {
        String key = RESET_TOKEN_PREFIX + email;
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Đã xóa Reset Token trong Redis của email: {}", email);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa Reset Token trong Redis của email: {}", email, e);
        }
    }

    private String buildKey(String username) {
        return REFRESH_TOKEN_PREFIX + username;
    }
}