package com.example.datn.DTO.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ClassSectionGradeUpdateRequest {
    @NotEmpty(message = "Danh sách điểm không được để trống")
    @Valid
    private List<StudentGradeRequest> grades;
}
