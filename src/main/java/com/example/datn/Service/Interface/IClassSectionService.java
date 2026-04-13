package com.example.datn.Service.Interface;

import com.example.datn.DTO.Response.ClassSectionResponse;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import com.example.datn.DTO.Response.SubjectResponse;

public interface IClassSectionService {
    void downloadTemplate(HttpServletResponse response) throws IOException;
    String importClassSections( MultipartFile file);
    List<SubjectResponse> getOpenedSubjectsBySemester(UUID semesterId);

    com.example.datn.DTO.Response.ClassSectionResponse updateClassSection(UUID id, com.example.datn.DTO.Request.ClassSectionUpdateRequest request);
    void deleteClassSection(UUID id);
    com.example.datn.DTO.Response.ClassSectionResponse getClassSectionById(UUID id);
    List<com.example.datn.DTO.Response.ClassSectionResponse> getClassSectionsBySubjectId(UUID subjectId);

    // RESTful State Transitions
    void approveClassSection(UUID id);
    void cancelClassSection(UUID id);
    void closeClassSection(UUID id);
}
