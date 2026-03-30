package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.SectionDefaultRequest;
import com.example.datn.DTO.Response.SectionDefaultResponse;

import java.util.List;
import java.util.UUID;

public interface ISectionDefaultService {
    SectionDefaultResponse createSectionDefault(SectionDefaultRequest request);
    SectionDefaultResponse updateSectionDefault(UUID id, SectionDefaultRequest request);
    void deleteSectionDefault(UUID id);
    List<SectionDefaultResponse> getBySubjectGroupId(UUID subjectGroupId);
    SectionDefaultResponse getById(UUID id);
}