package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminClassResponse {
    private UUID id;
    private String name;

    // Đã giấu đi các trường thừa thãi của Major và Cohort, chỉ lấy ID và Tên
    private UUID majorId;
    private String majorName;

    private UUID cohortId;
    private String cohortName;
}