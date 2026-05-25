package com.example.datn.Controller;

import com.example.datn.DTO.Request.CohortRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.ProgramCohortResponse;
import com.example.datn.Model.Cohort;
import com.example.datn.Service.Interface.ICohortService;
import com.example.datn.Service.Interface.IEducationProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cohort", description = "Quản lý khóa học (Khóa sinh viên)")
@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class CohortController {

    private final ICohortService cohortService;
    private final IEducationProgramService programService;

    @Operation(summary = "Tạo khóa học mới")
    @PostMapping
    public ApiResponse<Cohort> createCohort(@Valid @RequestBody CohortRequest request) {
        return ApiResponse.<Cohort>builder()
                .code(1000)
                .message("Tạo khóa học thành công")
                .result(cohortService.createCohort(request))
                .build();
    }

    @Operation(summary = "Lấy danh sách khóa học", description = "Cho phép tìm kiếm theo từ khóa")
    @GetMapping
    public ApiResponse<List<Cohort>> getAllCohorts(
            @RequestParam(value = "keyword", required = false) String keyword) {

        List<Cohort> resultList = (keyword != null && !keyword.trim().isEmpty())
                ? cohortService.searchCohorts(keyword)
                : cohortService.getAllCohorts();

        return ApiResponse.<List<Cohort>>builder()
                .code(1000)
                .message("Lấy danh sách khóa học thành công")
                .result(resultList)
                .build();
    }

    @Operation(summary = "Lấy thông tin khóa học theo ID")
    @GetMapping("/{id}")
    public ApiResponse<Cohort> getCohortById(@PathVariable UUID id) {
        return ApiResponse.<Cohort>builder()
                .code(1000)
                .message("Lấy thông tin khóa học thành công")
                .result(cohortService.getCohortById(id))
                .build();
    }

    @Operation(summary = "Cập nhật thông tin khóa học")
    @PutMapping("/{id}")
    public ApiResponse<Cohort> updateCohort(
            @PathVariable UUID id,
            @Valid @RequestBody CohortRequest request) {

        return ApiResponse.<Cohort>builder()
                .code(1000)
                .message("Cập nhật khóa học thành công")
                .result(cohortService.updateCohort(id, request))
                .build();
    }

    @Operation(summary = "Xóa khóa học")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCohort(@PathVariable UUID id) {
        cohortService.deleteCohort(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa khóa học thành công")
                .build();
    }

    /**
     * Lấy danh sách chương trình đào tạo đã gắn vào một khóa học cụ thể
     */
    @Operation(summary = "Lấy danh sách chương trình đào tạo theo khóa học")
    @GetMapping("/{id}/programs")
    public ApiResponse<List<ProgramCohortResponse>> getProgramsByCohort(@PathVariable UUID id) {
        return ApiResponse.<List<ProgramCohortResponse>>builder()
                .code(1000)
                .message("Lấy danh sách chương trình đào tạo theo khóa học thành công")
                .result(programService.getProgramsByCohort(id))
                .build();
    }
}