package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ProgramCohortRequest {

    @NotNull(message = "ID chương trình đào tạo không được để trống")
    private UUID programId;

    @NotNull(message = "ID khóa học không được để trống")
    private UUID cohortId;

    private LocalDate appliedDate;
}
