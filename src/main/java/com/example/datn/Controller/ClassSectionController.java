package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Service.Interface.IClassSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/class-sections")
public class ClassSectionController {

    @Autowired
    private IClassSectionService classSectionService;

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        classSectionService.downloadTemplate(response);
    }

    @PostMapping("/import/{semesterId}")
    public ApiResponse<String> importClassSections(
            @PathVariable UUID semesterId,
            @RequestParam("file") MultipartFile file) {
        
        String resultMessage = classSectionService.importClassSections(semesterId, file);
        
        return ApiResponse.<String>builder()
                .code(200)
                .message("Import thành công")
                .result(resultMessage)
                .build();
    }
}
