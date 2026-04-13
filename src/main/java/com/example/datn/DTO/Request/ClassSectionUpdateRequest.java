package com.example.datn.DTO.Request;

import com.example.datn.ENUM.SectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSectionUpdateRequest {
    @Min(value = 1, message = "Sĩ số tối đa phải lớn hơn 0")
    private Integer capacity;
    
    @Min(value = 0, message = "Sĩ số tối thiểu không được âm")
    private Integer minStudents;
}
