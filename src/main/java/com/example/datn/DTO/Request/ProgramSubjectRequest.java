package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ProgramSubjectRequest {
    @NotNull(message = "ID môn học không được để trống")
    private UUID subjectId;

    @NotNull(message = "ID đoạn nhóm môn học không được để trống")
    private UUID sectionId;

    @NotNull(message = "Học kỳ không được để trống")
    private Integer defaultSemester;

    private Double weight = 1.0; // Mặc định là 1.0 nếu không nhập
}