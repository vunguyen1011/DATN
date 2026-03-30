package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.ProgramSubjectRequest;
import com.example.datn.DTO.Response.ProgramSubjectResponse;

import java.util.List;
import java.util.UUID;

public interface IProgramSubjectService {
    ProgramSubjectResponse create(ProgramSubjectRequest request);
    ProgramSubjectResponse update(UUID id, ProgramSubjectRequest request);
    ProgramSubjectResponse getById(UUID id);
    List<ProgramSubjectResponse> getBySectionId(UUID sectionId);
    void delete(UUID id);
}