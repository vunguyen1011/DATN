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

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> importClassSections(

            @RequestParam("file") MultipartFile file) {
        
        String resultMessage = classSectionService.importClassSections(file);
        
        return ApiResponse.<String>builder()
                .code(200)
                .message("Import thành công")
                .result(resultMessage)
                .build();
    }

    @GetMapping("/semester/{semesterId}/subjects")
    public ApiResponse<java.util.List<com.example.datn.DTO.Response.SubjectResponse>> getOpenedSubjectsBySemester(@PathVariable UUID semesterId) {
        return ApiResponse.<java.util.List<com.example.datn.DTO.Response.SubjectResponse>>builder()
                .code(1000)
                .message("Lấy danh sách môn học đã mở trong học kỳ thành công")
                .result(classSectionService.getOpenedSubjectsBySemester(semesterId))
                .build();
    }

    @GetMapping("/opened-subjects")
    public ApiResponse<org.springframework.data.domain.Page<com.example.datn.DTO.Response.SubjectResponse>> getOpenedSubjectsPage(
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponse.<org.springframework.data.domain.Page<com.example.datn.DTO.Response.SubjectResponse>>builder()
                .code(1000)
                .message("Lấy danh sách môn học đã mở thành công")
                .result(classSectionService.getOpenedSubjectsPage(semesterId, search, pageable))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<com.example.datn.DTO.Response.ClassSectionResponse> updateClassSection(
            @PathVariable UUID id,
            @RequestBody com.example.datn.DTO.Request.ClassSectionUpdateRequest request) {
        return ApiResponse.<com.example.datn.DTO.Response.ClassSectionResponse>builder()
                .code(1000)
                .message("Cập nhật thông tin Lớp học phần thành công")
                .result(classSectionService.updateClassSection(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteClassSection(@PathVariable UUID id) {
        classSectionService.deleteClassSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa lớp học phần thành công")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<com.example.datn.DTO.Response.ClassSectionResponse> getClassSectionById(@PathVariable UUID id) {
        return ApiResponse.<com.example.datn.DTO.Response.ClassSectionResponse>builder()
                .code(1000)
                .message("Lấy thông tin chi tiết Lớp học phần thành công")
                .result(classSectionService.getClassSectionById(id))
                .build();
    }

    @GetMapping("/subject/{subjectId}")
    public ApiResponse<java.util.List<com.example.datn.DTO.Response.ClassSectionResponse>> getClassSectionsBySubjectIdAndSemesterId(
            @PathVariable UUID subjectId
            ) {
        return ApiResponse.<java.util.List<com.example.datn.DTO.Response.ClassSectionResponse>>builder()
                .code(1000)
                .message("Lấy danh sách Lớp học phần theo Môn học và Học kỳ thành công")
                .result(classSectionService.getClassSectionsBySubjectIdAndSemesterId(subjectId))
                .build();
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<Void> approveClassSection(@PathVariable UUID id) {
        classSectionService.approveClassSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Phê duyệt (Mở) lớp học phần thành công")
                .build();
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<Void> closeClassSection(@PathVariable UUID id) {
        classSectionService.closeClassSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Tạm đóng lớp học phần thành công")
                .build();
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<Void> cancelClassSection(@PathVariable UUID id) {
        classSectionService.cancelClassSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Hủy (Cancel) lớp học phần thành công")
                .build();
    }

}
