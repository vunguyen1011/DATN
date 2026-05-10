package com.example.datn.DTO.Request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FinalGradeRequest {

    @NotNull(message = "enrollmentId không được để trống")
    private UUID enrollmentId;

    @NotNull(message = "Điểm cuối kỳ không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm không được nhỏ hơn 0.0")
    @DecimalMax(value = "10.0", message = "Điểm không được lớn hơn 10.0")
    private Double finalScore;
}