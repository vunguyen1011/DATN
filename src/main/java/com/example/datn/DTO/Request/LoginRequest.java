package com.example.datn.DTO.Request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username; // Với sinh viên, đây sẽ là Mã sinh viên

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

}