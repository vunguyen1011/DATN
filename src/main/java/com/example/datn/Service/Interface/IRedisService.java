package com.example.datn.Service.Interface;

import java.time.Duration;

public interface IRedisService {

    void saveRefreshToken(String username, String refreshToken, Duration duration);

    String getRefreshToken(String username);

    void deleteRefreshToken(String username);

    boolean isValidRefreshToken(String username, String refreshToken);

    void saveOtp(String email, String otp);

    String getOtp(String email);

    void deleteOtp(String email);

    void saveResetToken(String email, String resetToken, Duration duration);

    String getResetToken(String email);

    void deleteResetToken(String email);

    void saveRecommendation(String studentId, String jsonResponse, Duration duration);

    String getRecommendation(String studentId);

    /**
     * Đồng bộ sĩ số CÒN LẠI của toàn bộ lớp học phần trong một học kỳ lên Redis.
     * PHẢI gọi API này TRƯỚC KHI mở đợt đăng ký để Redis có data.
     * 
     * @param semesterId ID học kỳ cần đồng bộ
     */
    void syncClassCapacityToRedis(java.util.UUID semesterId);

    int tryAcquireSlot(java.util.UUID theoryId, java.util.UUID labId, java.util.UUID studentId, java.util.UUID subjectId, String theoryMask, String labMask);

    void releaseSlot(java.util.UUID classSectionId, java.util.UUID studentId, java.util.UUID subjectId, String classMask);
    
    String recalculateAndCacheClassMask(java.util.UUID classSectionId);

    void clearRegistrationData();

    long incrementAndCheckRateLimit(String key, int windowInSeconds);

    void addPendingRegistration(java.util.UUID studentId, java.util.UUID classSectionId);
    void removePendingRegistration(java.util.UUID studentId, java.util.UUID classSectionId);
    void expirePendingRegistration(java.util.UUID studentId, java.util.UUID classSectionId, java.time.Duration duration);
    java.util.Set<java.util.UUID> getPendingRegistrations(java.util.UUID studentId);

    void addPendingCancellation(java.util.UUID studentId, java.util.UUID classSectionId);
    void removePendingCancellation(java.util.UUID studentId, java.util.UUID classSectionId);
    void expirePendingCancellation(java.util.UUID studentId, java.util.UUID classSectionId, java.time.Duration duration);
    java.util.Set<java.util.UUID> getPendingCancellations(java.util.UUID studentId);
}