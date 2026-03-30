package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class EducationProgramResponse {
    private UUID id;

    // --- Các trường mới thêm ---
    private String code;          // Mã chương trình (VD: CT-KTPM-K18)
    private Integer totalCredits; // Tổng số tín chỉ (Đã gộp từ min/max)
    private Boolean isTemplate;   // Cờ xác định đây có phải Template mẫu hay không

    private String name;
    private Float durationYears;

    // --- Thông tin Ngành ---
    private UUID majorId;
    private String majorName; // Trả về tên ngành cho Front-end dễ hiển thị

    private Boolean isActive;
}