package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.StudentUpdateRequest;
import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.StudentMapper;
import com.example.datn.Model.Student;
import com.example.datn.Model.User;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService implements IStudentService {
    
    private final StudentRepository studentRepository;
    private final CohortRepository cohortRepository;
    private final MajorRepository majorRepository;
    private final AdminClassRepository adminClassRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserProfileResponse.StudentProfile updateStudentProfile(UUID studentId, StudentUpdateRequest request) {
        User user =userRepository.findById(studentId).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        Student student = studentRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)); 
        
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            student.setFullName(request.getFullName().trim());
            if (student.getUser() != null) {
                student.getUser().setFullName(request.getFullName().trim());
            }
        }
        if (request.getPhone() != null) student.setPhone(request.getPhone());
        if (request.getAddress() != null) student.setAddress(request.getAddress());
        if (request.getGender() != null) student.setGender(request.getGender());
        if (request.getStatus() != null) student.setStatus(request.getStatus());

        if (request.getCohortId() != null) {
            student.setCohort(cohortRepository.findById(request.getCohortId())
                    .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND)));
        }
        if (request.getMajorId() != null) {
            student.setMajor(majorRepository.findById(request.getMajorId())
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND)));
        }
        if (request.getAdminClassId() != null) {
            student.setAdminClass(adminClassRepository.findById(request.getAdminClassId())
                    .orElseThrow(() -> new AppException(ErrorCode.ADMIN_CLASS_NOT_FOUND)));
        }
        
        student = studentRepository.save(student);
        
        return StudentMapper.toStudentProfile(student);
    }
}
