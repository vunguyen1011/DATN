package com.example.datn.Mapper;

import com.example.datn.DTO.Response.UserProfileResponse.StudentProfile;
import com.example.datn.Model.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {
    public static StudentProfile toStudentProfile(Student student) {
        if (student == null) return null;
        return StudentProfile.builder()
                .studentCode(student.getStudentCode())
                .phone(student.getPhone())
                .address(student.getAddress())
                .gender(student.getGender() != null ? student.getGender().name() : null)
                .status(student.getStatus() != null ? student.getStatus().name() : null)
                .cohortId(student.getCohort().getId())
                .cohortName(student.getCohort() != null ? student.getCohort().getName() : null)
                .majorId(student.getMajor().getId())
                .majorName(student.getMajor() != null ? student.getMajor().getName() : null)
                .adminClassName(student.getAdminClass() != null ? student.getAdminClass().getName() : null)
                .build();
    }
}
