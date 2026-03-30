package com.example.datn.Service.Impl;

import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.Lecturer;
import com.example.datn.Model.Student;
import com.example.datn.Model.User;
import com.example.datn.Repository.StudentRepository;
import com.example.datn.Repository.UserRepository;
import com.example.datn.Service.Interface.IUserService;
import com.example.datn.Repository.LecturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;

    @Override
    public UserProfileResponse getMyInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserProfileResponse response = UserProfileResponse.builder()
                .accountId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .build();

        Optional<Lecturer> lecturerOpt = lecturerRepository.findByUser(user);
        if (lecturerOpt.isPresent()) {
            Lecturer lecturer = lecturerOpt.get();
            response.setRole("LECTURER");
            response.setLecturerInfo(UserProfileResponse.LecturerProfile.builder()
                    .lecturerCode(lecturer.getLecturerCode())
                    .phone(lecturer.getPhone())
                    .address(lecturer.getAddress())
                    .degree(lecturer.getDegree())
                    .status(lecturer.getStatus() != null ? lecturer.getStatus().name() : null)
                    .facultyName(lecturer.getFaculty() != null ? lecturer.getFaculty().getName() : null)
                    .build());
            return response;
        }

        Optional<Student> studentOpt = studentRepository.findByUser(user);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            response.setRole("STUDENT");
            response.setStudentInfo(UserProfileResponse.StudentProfile.builder()
                    .studentCode(student.getStudentCode())
                    .phone(student.getPhone())
                    .address(student.getAddress())
                    .gender(student.getGender() != null ? student.getGender().name() : null)
                    .status(student.getStatus() != null ? student.getStatus().name() : null)
                    .cohortName(student.getCohort() != null ? student.getCohort().getName() : null)
                    .majorName(student.getMajor() != null ? student.getMajor().getName() : null)
                    .adminClassName(student.getAdminClass() != null ? student.getAdminClass().getName() : null)
                    .build());
            return response;
        }

        response.setRole("ADMIN");
        return response;
    }
}