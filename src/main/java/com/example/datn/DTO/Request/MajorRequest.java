package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NonNull;

@Data
public class MajorRequest {
    @NotBlank(message = "Tên ngành không được để trống")
    private  String name;
    @NotBlank(message = "Mã ngành không được để trống")
    private String code;
}
