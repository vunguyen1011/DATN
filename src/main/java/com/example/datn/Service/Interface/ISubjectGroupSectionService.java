package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.SubjectGroupSectionRequest;
import com.example.datn.DTO.Response.SubjectGroupSectionResponse;

import java.util.List;
import java.util.UUID;

public interface ISubjectGroupSectionService {
    SubjectGroupSectionResponse createSection(SubjectGroupSectionRequest request);

    SubjectGroupSectionResponse updateSection(UUID id, SubjectGroupSectionRequest request);

    SubjectGroupSectionResponse getSectionById(UUID id);


    void softDeleteSection(UUID id);
    List<SubjectGroupSectionResponse> getSectionsByProgramId(UUID programId);
}