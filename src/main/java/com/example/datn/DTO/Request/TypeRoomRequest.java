package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotBlank;

public class TypeRoomRequest {
    @NotBlank(message = "Mã loại phòng không được để trống")
    private String code;

    @NotBlank(message = "Tên loại phòng không được để trống")
    private String name;
}
