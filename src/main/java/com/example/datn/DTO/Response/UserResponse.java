package com.example.datn.DTO.Response;
import com.example.datn.Model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String username; // Mã sinh viên
    private String email;
    private String fullName;
    private String avatarUrl;
    private Boolean isActive;
    private boolean isLocked;
    private LocalDateTime createdAt;


}