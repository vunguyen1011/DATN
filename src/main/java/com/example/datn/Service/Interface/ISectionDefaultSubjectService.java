package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.SectionDefaultSubjectRequest;
import com.example.datn.DTO.Response.SectionDefaultSubjectResponse;
import com.example.datn.DTO.Response.TemplateTreeResponse;

import java.util.List;
import java.util.UUID;

public interface ISectionDefaultSubjectService {
    SectionDefaultSubjectResponse create(SectionDefaultSubjectRequest request);
    void delete(UUID sectionDefaultId, UUID subjectId);
    List<SectionDefaultSubjectResponse> getBySectionDefaultId(UUID sectionDefaultId);
    SectionDefaultSubjectResponse update(UUID id, SectionDefaultSubjectRequest request);
    TemplateTreeResponse getTemplateTree();
}