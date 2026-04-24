package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.StudentUpdateRequest;
import com.example.datn.DTO.Response.ProgramSubjectResponse;
import com.example.datn.DTO.Response.ProgramTreeResponse;
import com.example.datn.DTO.Response.UserProfileResponse.StudentProfile;

import java.util.List;
import java.util.UUID;

public interface IStudentService {
    StudentProfile updateStudentProfile(UUID studentId, StudentUpdateRequest request);

    void exportStudentsToPdf(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException;

    List<ProgramSubjectResponse> getMyProgram();

    ProgramTreeResponse getMyProgramTree();
}
