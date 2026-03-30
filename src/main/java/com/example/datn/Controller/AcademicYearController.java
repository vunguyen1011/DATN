package com.example.datn.Controller;

import com.example.datn.DTO.Request.AcademicYearRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Model.AcademicYear;
import com.example.datn.Service.Interface.IAcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final IAcademicYearService academicYearService;

    @PostMapping
    public ApiResponse<AcademicYear> createAcademicYear(@Valid @RequestBody AcademicYearRequest request) {
        return ApiResponse.<AcademicYear>builder()
                .code(1000)
                .message("Tạo năm học thành công")
                .result(academicYearService.createAcademicYear(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<AcademicYear>> getAllAcademicYears(
            @RequestParam(value = "keyword", required = false) String keyword) {

        List<AcademicYear> resultList = (keyword != null && !keyword.trim().isEmpty())
                ? academicYearService.searchAcademicYears(keyword)
                : academicYearService.getAllAcademicYears();

        return ApiResponse.<List<AcademicYear>>builder()
                .code(1000)
                .message("Lấy danh sách năm học thành công")
                .result(resultList)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AcademicYear> getAcademicYearById(@PathVariable UUID id) {
        return ApiResponse.<AcademicYear>builder()
                .code(1000)
                .message("Lấy thông tin năm học thành công")
                .result(academicYearService.getAcademicYearById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AcademicYear> updateAcademicYear(
            @PathVariable UUID id,
            @Valid @RequestBody AcademicYearRequest request) {

        return ApiResponse.<AcademicYear>builder()
                .code(1000)
                .message("Cập nhật năm học thành công")
                .result(academicYearService.updateAcademicYear(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAcademicYear(@PathVariable UUID id) {
        academicYearService.deleteAcademicYear(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa năm học thành công")
                .build();
    }
}