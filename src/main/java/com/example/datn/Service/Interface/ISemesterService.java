package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.SemesterRequest;
import com.example.datn.DTO.Response.SemesterResponse;

import java.util.List;
import java.util.UUID;

public interface ISemesterService {
    SemesterResponse createSemester(SemesterRequest request);
    List<SemesterResponse> getAllSemesters();
    SemesterResponse getSemesterById(UUID id);
    SemesterResponse updateSemester(UUID id, SemesterRequest request);
    void deleteSemester(UUID id);
    List<SemesterResponse> searchSemesters(String keyword);
}