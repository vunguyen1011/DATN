package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.AcademicYearRequest;
import com.example.datn.Model.AcademicYear;

import java.util.List;
import java.util.UUID;

public interface IAcademicYearService {
    AcademicYear createAcademicYear(AcademicYearRequest request);
    List<AcademicYear> getAllAcademicYears();
    AcademicYear getAcademicYearById(UUID id);
    AcademicYear updateAcademicYear(UUID id, AcademicYearRequest request);
    void deleteAcademicYear(UUID id);
    List<AcademicYear> searchAcademicYears(String keyword);
}