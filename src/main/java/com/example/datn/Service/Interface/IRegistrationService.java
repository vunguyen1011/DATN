package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.EnrollRequest;
import com.example.datn.DTO.Response.EnrollmentResponse;
import com.example.datn.DTO.Response.RegistrationStatusResponse;
import com.example.datn.DTO.Response.SubjectResponse;

import java.util.List;
import java.util.UUID;

public interface IRegistrationService {
    RegistrationStatusResponse getRegistrationStatus();
    List<com.example.datn.DTO.Response.EnrollmentSimpleResponse> enroll(EnrollRequest request);
    void cancelEnrollment(UUID classSectionId);
    List<EnrollmentResponse> getMyTimetable();
}
