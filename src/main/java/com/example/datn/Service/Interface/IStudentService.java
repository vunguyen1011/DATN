package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.StudentUpdateRequest;
import com.example.datn.DTO.Response.UserProfileResponse.StudentProfile;

import java.util.UUID;

public interface IStudentService {
    StudentProfile updateStudentProfile(UUID studentId, StudentUpdateRequest request);
}
