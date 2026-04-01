package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.SubjectComponentRequest;
import com.example.datn.DTO.Response.SubjectComponentResponse;

import java.util.List;
import java.util.UUID;

public interface ISubjectComponentService {
    SubjectComponentResponse createSubjectComponent(SubjectComponentRequest request);
    SubjectComponentResponse updateSubjectComponent(UUID id, SubjectComponentRequest request);
    SubjectComponentResponse getSubjectComponentById(UUID id);
    List<SubjectComponentResponse> getComponentsBySubjectId(UUID subjectId);
    void deleteSubjectComponent(UUID id);
}
