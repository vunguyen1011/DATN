package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AdminClassRequest {

    @NotBlank(message = "Tên lớp không được để trống")
    private String name;


    @NotNull(message = "ID Ngành học không được để trống")
    private UUID majorId;

    @NotNull(message = "ID Khóa học không được để trống")
    private UUID cohortId;
}