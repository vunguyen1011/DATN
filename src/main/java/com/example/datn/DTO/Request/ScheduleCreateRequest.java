package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * [ADMIN] Tạo lịch học ban đầu cho một lớp học phần.
 * Bước 1 trong quy trình xếp lịch:
 *   Admin chỉ xác định ClassSection + Thời gian (Ngày/Tiết).
 *   Phòng học là tuỳ chọn — có thể xếp ngay hoặc xếp sau qua PATCH /room.
 *   Giảng viên KHÔNG thuộc bước này — HOD phân công riêng qua PATCH /lecturer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleCreateRequest {

    @NotNull(message = "Mã lớp học phần không được để trống")
    private UUID classSectionId;

    @NotNull(message = "Thứ trong tuần không được để trống")
    @Min(value = 2, message = "Thứ phải từ 2 (Thứ 2) đến 8 (Chủ nhật)")
    @Max(value = 8, message = "Thứ phải từ 2 (Thứ 2) đến 8 (Chủ nhật)")
    private Integer dayOfWeek;

    @NotNull(message = "Tiết bắt đầu không được để trống")
    @Min(value = 1, message = "Tiết bắt đầu phải >= 1")
    private Integer startPeriod;

    // nullable — có thể chưa xác định số tiết ngay
    private Integer endPeriod;

    // optional — có thể xếp phòng ngay lúc tạo hoặc để null rồi xếp sau
    private UUID roomId;
}
