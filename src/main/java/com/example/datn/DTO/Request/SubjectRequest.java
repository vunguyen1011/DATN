package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectRequest {

    @NotBlank(message = "Mã môn học không được để trống")
    private String code;

    @NotBlank(message = "Tên môn học không được để trống")
    private String name;

    @NotNull(message = "Số tín chỉ không được để trống")
    @Min(value = 1, message = "Số tín chỉ phải lớn hơn 0")
    private Integer credits;

    @NotBlank(message = "Bộ môn/Khoa quản lý không được để trống")
    private String departmentName;

    @Min(value = 0, message = "Số tiết lý thuyết không được âm")
    private Integer theoryPeriod;

    @Min(value = 0, message = "Số tiết thực hành không được âm")
    private Integer practicePeriod;
    @NotNull(message = "Hệ số không được để trống")
    @Min(value =0, message = "Hệ số phải lớn hơn hoặc bằng 0")
    private  Double coffe;
}