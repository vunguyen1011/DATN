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
    List<ProgramSubjectResponse> getFlattenedByProgramId(UUID programId);
    List<ProgramSubjectResponse> getSubjectsByCohortAndMajor(UUID cohortId, UUID majorId);
    org.springframework.data.domain.Page<ProgramSubjectResponse> getSubjectsByCohortAndMajorPage(UUID cohortId, UUID majorId, String keyword, org.springframework.data.domain.Pageable pageable);
    List<ProgramSubjectResponse> getOpenedSubjectsForStudent(UUID cohortId, UUID majorId);
    org.springframework.data.domain.Page<ProgramSubjectResponse> getOpenedSubjectsForStudentPage(UUID cohortId, UUID majorId, String keyword, org.springframework.data.domain.Pageable pageable);
    void delete(UUID sectionId, UUID subjectId);
}