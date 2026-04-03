package com.example.datn.Service.Interface;

import com.example.datn.DTO.Response.ClassSectionResponse;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

public interface IClassSectionService {
    void downloadTemplate(HttpServletResponse response) throws IOException;
    String importClassSections(UUID semesterId, MultipartFile file);
}
