package com.example.datn.DTO.Request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class StudentGradeRequest {

    @NotNull(message = "enrollmentId không được để trống")
    private UUID enrollmentId;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double midtermScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double finalScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double totalScore;

    // Admin có thể nhập thẳng isPassed hoặc để system tự tính (totalScore >= 5.0)
    private Boolean isPassed;
}
