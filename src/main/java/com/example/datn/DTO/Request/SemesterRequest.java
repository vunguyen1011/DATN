package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class SemesterRequest {

    @NotBlank(message = "Tên học kỳ không được để trống (VD: Học kỳ 1)")
    private String name;

    @NotNull(message = "ID Năm học không được để trống")
    private UUID academicYearId;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;
}