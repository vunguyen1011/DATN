package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SectionDefaultRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotNull(message = "Tính bắt buộc không được để trống")
    private Boolean isMandatory;

    @Min(value = 0, message = "Số tín chỉ yêu cầu không được âm")
    private Integer requiredCredits=0;

    @Min(value = 1, message = "Vị trí hiển thị phải từ 1")
    private Integer index;

    @NotNull(message = "ID Khối Hồng không được để trống")
    private UUID subjectGroupId;
}