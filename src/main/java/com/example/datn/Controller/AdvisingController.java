package com.example.datn.Controller;

import com.example.datn.DTO.Response.RecommendationResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.Student;
import com.example.datn.Repository.StudentRepository;
import com.example.datn.Service.Interface.IAiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Advising / Recommendation", description = "Các API gợi ý và tư vấn học tập sử dụng AI")
@RestController
@RequestMapping("/api/v1/advising")
@RequiredArgsConstructor
public class AdvisingController {

    private final IAiRecommendationService aiRecommendationService;
    private final StudentRepository studentRepository;

    @Operation(summary = "Lấy danh sách gợi ý môn học từ AI", description = "Dựa trên kết quả học tập và thông tin của sinh viên đang đăng nhập")
    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getRecommendations() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentRepository.findByUser_Username(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(aiRecommendationService.getRecommendations(student.getId()));
    }
}
