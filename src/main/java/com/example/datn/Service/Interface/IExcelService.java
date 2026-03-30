package com.example.datn.Service.Interface;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IExcelService {
    public void downloadTemplate(HttpServletResponse response) throws IOException;
    String  saveUsersFromExcel(MultipartFile file);
}
