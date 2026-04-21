package com.example.datn.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkScheduleLecturerRequest {
    
    // Danh sách các lịch cần phân công
    private List<UUID> scheduleIds;

    // Mã giảng viên (nullable để hủy phân công hàng loạt)
    private String lecturerCode;
}
