package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterResponse {
    private UUID id;
    private String name;

    // Giấu Entity AcademicYear đi, chỉ trả về ID và Tên năm học (VD: 2023-2024)
    private UUID academicYearId;
    private String academicYearName;

    private LocalDate startDate;
    private LocalDate endDate;
}