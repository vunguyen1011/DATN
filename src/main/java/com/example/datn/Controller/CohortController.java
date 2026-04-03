package com.example.datn.Controller;

import com.example.datn.DTO.Request.CohortRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Model.Cohort;
import com.example.datn.Service.Interface.ICohortService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class    CohortController {

    private final ICohortService cohortService;

    @PostMapping
    public ApiResponse<Cohort> createCohort(@Valid @RequestBody CohortRequest request) {
        return ApiResponse.<Cohort>builder()
                .code(1000)
                .message("Tạo khóa học thành công")
                .result(cohortService.createCohort(request))
                .build();
    }

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

    @GetMapping("/{id}")
    public ApiResponse<Cohort> getCohortById(@PathVariable UUID id) {
        return ApiResponse.<Cohort>builder()
                .code(1000)
                .message("Lấy thông tin khóa học thành công")
                .result(cohortService.getCohortById(id))
                .build();
    }

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

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCohort(@PathVariable UUID id) {
        cohortService.deleteCohort(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa khóa học thành công")
                .build();
    }
}