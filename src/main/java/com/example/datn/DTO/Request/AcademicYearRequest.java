package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AcademicYearRequest {

    @NotBlank(message = "Tên năm học không được để trống")
    @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Định dạng năm học phải là YYYY-YYYY (VD: 2023-2024)")
    private String name;

    @NotNull(message = "Trạng thái năm học hiện tại không được để trống")
    private Boolean isCurrent;
}