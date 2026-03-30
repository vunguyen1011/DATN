package com.example.datn.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SubjectGroupRequest {

    @NotBlank(message = "Tên nhóm môn học không được để trống")
    private String name;

     Boolean isGlobal;
     @Min(value = 0, message = "Vị trí phải là số nguyên dương")
     private int index;


}