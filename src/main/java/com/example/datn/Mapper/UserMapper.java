package com.example.datn.Mapper;

import com.example.datn.DTO.Response.UserResponse;
import com.example.datn.Model.User;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component

public class UserMapper {

    public static UserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .isLocked(user.isLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
