package com.example.datn.Controller;

import com.example.datn.DTO.Request.SemesterRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SemesterResponse;
import com.example.datn.Service.Interface.ISemesterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final ISemesterService semesterService;

    @PostMapping
    public ApiResponse<SemesterResponse> createSemester(@Valid @RequestBody SemesterRequest request) {
        return ApiResponse.<SemesterResponse>builder()
                .code(1000)
                .message("Tạo học kỳ thành công")
                .result(semesterService.createSemester(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<SemesterResponse>> getAllSemesters(
            @RequestParam(value = "keyword", required = false) String keyword) {

        List<SemesterResponse> resultList = (keyword != null && !keyword.trim().isEmpty())
                ? semesterService.searchSemesters(keyword)
                : semesterService.getAllSemesters();

        return ApiResponse.<List<SemesterResponse>>builder()
                .code(1000)
                .message("Lấy danh sách học kỳ thành công")
                .result(resultList)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SemesterResponse> getSemesterById(@PathVariable UUID id) {
        return ApiResponse.<SemesterResponse>builder()
                .code(1000)
                .message("Lấy thông tin học kỳ thành công")
                .result(semesterService.getSemesterById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<SemesterResponse> updateSemester(
            @PathVariable UUID id,
            @Valid @RequestBody SemesterRequest request) {

        return ApiResponse.<SemesterResponse>builder()
                .code(1000)
                .message("Cập nhật học kỳ thành công")
                .result(semesterService.updateSemester(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSemester(@PathVariable UUID id) {
        semesterService.deleteSemester(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa học kỳ thành công")
                .build();
    }
    @GetMapping("/current")
    public ApiResponse<SemesterResponse> getCurrentSemester() {
        return ApiResponse.<SemesterResponse>builder()
                .code(1000)
                .message("Lấy học kỳ hiện tại thành công")
                .result(semesterService.findCurrentSemester())
                .build();
    }
}