package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {

    @NotNull(message = "Mã lớp học phần không được để trống")
    private UUID classSectionId;

    // Phòng học (nullable - có thể chưa xếp)
    private UUID roomId;

    // Giảng viên (nullable - có thể chưa phân công)
    private UUID lecturerId;

    @NotNull(message = "Thứ trong tuần không được để trống")
    @Min(value = 2, message = "Thứ phải từ 2 (Thứ 2) đến 8 (Chủ nhật)")
    @Max(value = 8, message = "Thứ phải từ 2 (Thứ 2) đến 8 (Chủ nhật)")
    private Integer dayOfWeek; // 2 -> 8

    @NotNull(message = "Tiết bắt đầu không được để trống")
    @Min(value = 1, message = "Tiết bắt đầu phải >= 1")
    private Integer startPeriod;

    // Tiết kết thúc (nullable - cho phép chưa xác định)
    private Integer endPeriod;
}
