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
}