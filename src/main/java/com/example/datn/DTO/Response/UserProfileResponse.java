package com.example.datn.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {
    private UUID accountId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Boolean isActive;

    // Đổi String role thành List<String> roles để chứa được nhiều quyền
    private List<String> roles;

    private LecturerProfile lecturerInfo;
    private StudentProfile studentInfo;

    @Data
    @Builder
    public static class LecturerProfile {
        private String lecturerCode;
        private String phone;
        private String address;
        private String degree;
        private String status;
        private UUID facultyId;
        private String facultyName;
        private UUID majorId;
        private String majorName;
    }

    @Data
    @Builder
    public static class StudentProfile {
        private String studentCode;
        private String phone;
        private String address;
        private String gender;
        private String status;
        private UUID cohortId;
        private String cohortName;
        private UUID majorId;
        private String majorName;
        private String adminClassName;
    }
}