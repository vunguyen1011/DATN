package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CohortRequest {

    @NotBlank(message = "Tên khóa học không được để trống (VD: K34)")
    private String name;

    @NotNull(message = "Năm bắt đầu không được để trống")
    @Min(value = 1990, message = "Năm bắt đầu không hợp lệ (Phải lớn hơn 1990)")
    @Max(value = 2100, message = "Năm bắt đầu không hợp lệ")
    private Integer startYear;
}