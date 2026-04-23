package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerSuggestionResponse {

    // ID dùng để gọi API update thực tế
    private UUID lecturerId;

    // Code dùng để hiển thị (VD: GV001)
    private String lecturerCode;

    // Tên đầy đủ hiển thị trên UI
    private String fullName;

    // Tổng điểm chấm từ các ScoreRule (Dùng để sort trên Frontend)
    private int totalScore;

    // Chuỗi giải thích lý do tại sao người này lại được chọn (Dùng làm Tooltip trên UI)
    private String matchReason;
}