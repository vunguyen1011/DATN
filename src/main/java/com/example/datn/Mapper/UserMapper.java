package com.example.datn.Mapper;

import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.DTO.Response.UserResponse;
import com.example.datn.Model.Lecturer;
import com.example.datn.Model.Role;
import com.example.datn.Model.Student;
import com.example.datn.Model.User;
import java.util.Set;
import java.util.stream.Collectors;

public class UserMapper {

    private UserMapper() {}

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

    public static UserProfileResponse toUserProfileResponse(User user, Student student, Lecturer lecturer, Set<Role> roles) {
        if (user == null) {
            return null;
        }

        UserProfileResponse response = UserProfileResponse.builder()
                .accountId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .build();

        if (roles != null && !roles.isEmpty()) {
            response.setRoles(roles.stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));
        }

        if (lecturer != null) {
            response.setLecturerInfo(LecturerMapper.toLecturerProfile(lecturer));
        }

        if (student != null) {
            response.setStudentInfo(StudentMapper.toStudentProfile(student));
        }

        return response;
    }
}