package com.example.datn.Service.Impl;

import com.example.datn.Model.User;
import com.example.datn.Repository.StudentRepository;
import com.example.datn.Config.JwtProvider; // Nhớ import đúng package của bạn
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SupportService {
    private final StudentRepository studentRepository;

    // Tiêm thẳng JwtProvider (hoặc class chuyên tạo token của bạn) vào đây
    private final JwtProvider jwtProvider;

    public void createTokenCsvFile() {
        List<User> students = studentRepository.findAll()
                .stream()
                .filter(student -> student.getUser() != null)
                .map(student -> student.getUser())
                .toList();

        String csvFilePath = "D:/user.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {
            for (User user : students) {
                String token = jwtProvider.genAccessToken(user.getUsername());
                writer.println(token);
            }
            log.info("Exported {} tokens to {}", students.size(), csvFilePath);
        } catch (IOException e) {
            log.error("Error writing CSV file", e);
        }
    }
}