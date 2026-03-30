package com.example.datn.Service.Interface;

public interface IEmailService {
    void sendOtpEmail(String to, String otp);

    void sendSimpleMessage(String to, String subject, String text);
}
