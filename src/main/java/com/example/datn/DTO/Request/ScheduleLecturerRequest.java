package com.example.datn.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * [HOD] Phân công giảng viên vào một lịch học đã có.
 * Bước 3 trong quy trình xếp lịch.
 * HOD chỉ được chạm vào: Lecturer.
 * Trường Room/Time thuộc quyền Admin — KHÔNG thuộc request này.
 * lecturerId = null → huỷ phân công giảng viên khỏi lịch này.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleLecturerRequest {

    // nullable — cho phép huỷ phân công (lecturer = null)
    private UUID lecturerId;
}
