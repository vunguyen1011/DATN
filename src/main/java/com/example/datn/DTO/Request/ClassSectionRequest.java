package com.example.datn.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSectionRequest {
    @NotNull(message = "Mã môn học không được để trống")
    private String subjectCode;
    
    private Integer totalStudents;
    private Integer maxTheoryCapacity;
    private Integer maxPracticeCapacity;
}
