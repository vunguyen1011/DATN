package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.SubjectRequest;
import com.example.datn.DTO.Response.SubjectResponse;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISubjectService {
    SubjectResponse createSubject(SubjectRequest request);

    SubjectResponse updateSubject(UUID id, SubjectRequest request);

    Page<SubjectResponse> getAllSubjects(String keyword, Pageable pageable);

    SubjectResponse getSubjectById(UUID id);

    void deleteSubject(UUID id);

    List<SubjectResponse> getPrerequisites(UUID subjectId);

    void updatePrerequisites(UUID subjectId, List<UUID> prerequisiteIds);

    List<SubjectResponse> getDependentSubjects(UUID subjectId);

    List<SubjectResponse> getPrerequisiteTree(UUID subjectId);
}