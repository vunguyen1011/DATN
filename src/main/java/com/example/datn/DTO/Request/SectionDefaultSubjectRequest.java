package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SectionDefaultSubjectRequest {

    @NotNull(message = "ID Khối Cam mẫu không được để trống")
    private UUID sectionDefaultId;

    @NotNull(message = "ID Môn học không được để trống")
    private UUID subjectId;

    @Min(value = 1, message = "Học kỳ mặc định phải từ 1")
    private Integer defaultSemester;
}