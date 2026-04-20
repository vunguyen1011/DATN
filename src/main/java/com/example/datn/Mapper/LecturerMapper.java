package com.example.datn.Mapper;

import com.example.datn.DTO.Response.UserProfileResponse.LecturerProfile;
import com.example.datn.Model.Lecturer;
import org.springframework.stereotype.Component;

@Component
public class LecturerMapper {
    public static LecturerProfile toLecturerProfile(Lecturer lecturer) {
        if (lecturer == null) return null;
        return LecturerProfile.builder()
                .lecturerCode(lecturer.getLecturerCode())
                .phone(lecturer.getPhone())
                .majorId(lecturer.getMajor().getId())
                .majorName(lecturer.getMajor().getName())
                .address(lecturer.getAddress())
                .degree(lecturer.getDegree())
                .status(lecturer.getStatus() != null ? lecturer.getStatus().name() : null)
                .facultyName(lecturer.getFaculty() != null ? lecturer.getFaculty().getName() : null)
                .build();
    }
}
