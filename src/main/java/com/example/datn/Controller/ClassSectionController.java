package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SubjectResponse;
import com.example.datn.Service.Interface.IClassSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Class Section", description = "Quản lý lớp học phần")
@RestController
@RequestMapping("/api/class-sections")
public class ClassSectionController {

    @Autowired
    private IClassSectionService classSectionService;

    @Operation(summary = "Tải file mẫu Excel nhập lớp học phần")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        classSectionService.downloadTemplate(response);
    }

    @Operation(summary = "Nhập danh sách lớp học phần từ file Excel")
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

    @Operation(summary = "Lấy danh sách môn học đã mở trong học kỳ")
    @GetMapping("/semester/{semesterId}/subjects")
    public ApiResponse<java.util.List<com.example.datn.DTO.Response.SubjectResponse>> getOpenedSubjectsBySemester(@PathVariable UUID semesterId) {
        return ApiResponse.<java.util.List<com.example.datn.DTO.Response.SubjectResponse>>builder()
                .code(1000)
                .message("Lấy danh sách môn học đã mở trong học kỳ thành công")
                .result(classSectionService.getOpenedSubjectsBySemester(semesterId))
                .build();
    }

    @Operation(summary = "Lấy danh sách môn học đã mở có phân trang và tìm kiếm")
    @GetMapping("/opened-subjects")
    public ApiResponse<Page<SubjectResponse>> getOpenedSubjectsPage(
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<org.springframework.data.domain.Page<com.example.datn.DTO.Response.SubjectResponse>>builder()
                .code(1000)
                .message("Lấy danh sách môn học đã mở thành công")
                .result(classSectionService.getOpenedSubjectsPage(semesterId, search, pageable))
                .build();
    }

    @Operation(summary = "Cập nhật lớp học phần")
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

    @Operation(summary = "Xóa lớp học phần")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteClassSection(@PathVariable UUID id) {
        classSectionService.deleteClassSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa lớp học phần thành công")
                .build();
    }

    @Operation(summary = "Lấy thông tin chi tiết lớp học phần theo ID")
    @GetMapping("/{id}")
    public ApiResponse<com.example.datn.DTO.Response.ClassSectionResponse> getClassSectionById(@PathVariable UUID id) {
        return ApiResponse.<com.example.datn.DTO.Response.ClassSectionResponse>builder()
                .code(1000)
                .message("Lấy thông tin chi tiết Lớp học phần thành công")
                .result(classSectionService.getClassSectionById(id))
                .build();
    }

    @Operation(summary = "Lấy danh sách lớp học phần theo môn học và học kỳ")
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

    @Operation(summary = "Phê duyệt mở lớp học phần")
    @PatchMapping("/{id}/approve")
    public ApiResponse<Void> approveClassSection(@PathVariable UUID id) {
        classSectionService.approveClassSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Phê duyệt (Mở) lớp học phần thành công")
                .build();
    }

    @Operation(summary = "Tạm đóng lớp học phần")
    @PatchMapping("/{id}/close")
    public ApiResponse<Void> closeClassSection(@PathVariable UUID id) {
        classSectionService.closeClassSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Tạm đóng lớp học phần thành công")
                .build();
    }

    @Operation(summary = "Hủy bỏ lớp học phần")
    @PatchMapping("/{id}/cancel")
    public ApiResponse<Void> cancelClassSection(@PathVariable UUID id) {
        classSectionService.cancelClassSection(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Hủy (Cancel) lớp học phần thành công")
                .build();
    }

    @Operation(summary = "Lấy danh sách môn học đã mở trong khoa theo học kỳ")
    @GetMapping("/semester/{semesterId}/subjects-in-faculty")
    public ApiResponse<List<SubjectResponse>> getSubjectInFaculty(@PathVariable UUID semesterId) {
        return ApiResponse.<List<SubjectResponse>>builder()
                .code(1000)
                .message("Lấy danh sách môn học đã mở trong học kỳ thành công")
                .result(classSectionService.getSubjectInFaculty(semesterId))
                .build();
    }

    @Operation(summary = "Phê duyệt tất cả lớp học phần đang chờ trong học kỳ")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/approval-pending/{id}")
    public ApiResponse<Void> approvalPending(@PathVariable UUID id) {
        int pendingCount = classSectionService.approveAllPendingBySemester(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã phê duyêt " + pendingCount + " lớp học phần đang chờ phê duyệt trong học kỳ.")
                .build();
    }
    @Operation(summary = "Tìm kiếm môn học trong khoa theo học kỳ và từ khóa")
    @GetMapping("/semester/{semesterId}/subjects-in-faculty/search")
    public ApiResponse<List<SubjectResponse>> searchSubjectInFaculty(
            @PathVariable UUID semesterId,
            @RequestParam(required = false) String keyword
    ) {

        return ApiResponse.<List<SubjectResponse>>builder()
                .code(1000)
                .message("Tìm kiếm môn học trong khoa thành công")
                .result(classSectionService.searchSubjectInFaculty(semesterId, keyword))
                .build();
    }
}
