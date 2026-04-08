package com.example.datn.Service.Interface;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IExcelService {
     void downloadTemplate(HttpServletResponse response) throws IOException;
    String  saveUsersFromExcel(MultipartFile file);
    void downloadTemplateLecturer(HttpServletResponse response) throws IOException;
    String saveLecturersFromExcel(MultipartFile file);

}
