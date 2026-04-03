package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class EducationProgramRequest {
    @NotBlank(message = "Mã chương trình không được để trống")
    private String code;

    @NotBlank(message = "Tên chương trình không được để trống")
    private String name;

    @NotNull(message = "Tổng số tín chỉ không được để trống")
    @Min(value = 1, message = "Số tín chỉ phải lớn hơn 0")
    private Integer totalCredits;

    @NotNull(message = "Thời gian đào tạo không được để trống")
    private Float durationYears;

//    @NotNull(message = "Cờ Template không được để trống")
//    private Boolean isTemplate;

    @NotNull(message = "ID của Ngành học không được để trống")
    private UUID majorId;
}