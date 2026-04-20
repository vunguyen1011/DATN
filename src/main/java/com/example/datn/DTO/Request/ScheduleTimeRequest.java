package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [ADMIN] Xếp thời gian (thứ, tiết) cho một lịch đã có.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleTimeRequest {

    @NotNull(message = "Thứ trong tuần không được để trống")
    @Min(value = 2, message = "Thứ phải từ 2 đến 8")
    @Max(value = 8, message = "Thứ phải từ 2 đến 8")
    private Integer dayOfWeek;

    @NotNull(message = "Tiết bắt đầu không được để trống")
    @Min(value = 1, message = "Tiết bắt đầu phải >= 1")
    private Integer startPeriod;

}
