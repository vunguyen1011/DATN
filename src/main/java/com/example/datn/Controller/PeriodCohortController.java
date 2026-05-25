package com.example.datn.Controller;

import com.example.datn.DTO.Request.PeriodCohortRequest;
import com.example.datn.DTO.Request.PeriodCohortUpdateRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.PeriodCohortResponse;
import com.example.datn.Service.Interface.IPeriodCohortService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Period Cohort Configuration", description = "Cấu hình đối tượng được phép tham gia đợt đăng ký")
@RestController
@RequestMapping("/api/period-cohorts")
@RequiredArgsConstructor
public class PeriodCohortController {

    private final IPeriodCohortService periodCohortService;

    @Operation(summary = "Thêm cấu hình đối tượng (Khóa học) cho đợt đăng ký")
    @PostMapping
    public ApiResponse<PeriodCohortResponse> create(@RequestBody @Valid PeriodCohortRequest request) {
        return ApiResponse.<PeriodCohortResponse>builder()
                .result(periodCohortService.create(request))
                .build();
    }

    @Operation(summary = "Lấy toàn bộ danh sách cấu hình đối tượng đợt đăng ký")
    @GetMapping
    public ApiResponse<List<PeriodCohortResponse>> getAll() {
        return ApiResponse.<List<PeriodCohortResponse>>builder()
                .result(periodCohortService.getAll())
                .build();
    }

    @Operation(summary = "Lấy chi tiết cấu hình đối tượng theo ID")
    @GetMapping("/{id}")
    public ApiResponse<PeriodCohortResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<PeriodCohortResponse>builder()
                .result(periodCohortService.getById(id))
                .build();
    }

    @Operation(summary = "Lấy cấu hình đối tượng theo ID đợt đăng ký")
    @GetMapping("/period/{periodId}")
    public ApiResponse<List<PeriodCohortResponse>> getByRegistrationPeriodId(@PathVariable UUID periodId) {
        return ApiResponse.<List<PeriodCohortResponse>>builder()
                .result(periodCohortService.getByRegistrationPeriodId(periodId))
                .build();
    }

    @Operation(summary = "Cập nhật cấu hình đối tượng đợt đăng ký")
    @PutMapping("/{id}")
    public ApiResponse<PeriodCohortResponse> update(@PathVariable UUID id, @RequestBody @Valid PeriodCohortUpdateRequest request) {
        return ApiResponse.<PeriodCohortResponse>builder()
                .result(periodCohortService.update(id, request))
                .build();
    }

    @Operation(summary = "Xóa cấu hình đối tượng đợt đăng ký")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        periodCohortService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Deleted successfully")
                .build();
    }
}
