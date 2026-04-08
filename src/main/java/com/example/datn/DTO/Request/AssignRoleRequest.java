package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignRoleRequest {
    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Tên Role không được để trống (VD: ROLE_ADMIN)")
    private String roleName;
}