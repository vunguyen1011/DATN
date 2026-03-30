package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SubjectGroupSectionRequest {

    @NotNull(message = "ID Chương trình đào tạo không được để trống")
    private UUID educationProgramId;

    @NotNull(message = "ID Nhóm môn học không được để trống")
    private UUID subjectGroupId;

    @NotBlank(message = "Tên đoạn (tiêu đề) không được để trống")
    @Size(max = 500, message = "Tiêu đề không được vượt quá 500 ký tự")
    private String title;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @NotNull(message = "Thứ tự hiển thị (index) không được để trống")
    @Min(value = 0, message = "Thứ tự phải lớn hơn hoặc bằng 0")
    private Integer index;

    @NotNull(message = "Vui lòng xác định đoạn này là Bắt buộc (true) hay Tự chọn (false)")
    private Boolean isMandatory;

    @Min(value = 0, message = "Số tín chỉ phải lớn hơn hoặc bằng 0")
    private Integer requiredCredits = 0;
}