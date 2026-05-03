package com.example.datn.Controller;

import com.example.datn.DTO.Request.PeriodCohortRequest;
import com.example.datn.DTO.Request.PeriodCohortUpdateRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.PeriodCohortResponse;
import com.example.datn.Service.Interface.IPeriodCohortService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/period-cohorts")
@RequiredArgsConstructor
public class PeriodCohortController {

    private final IPeriodCohortService periodCohortService;

    @PostMapping
    public ApiResponse<PeriodCohortResponse> create(@RequestBody @Valid PeriodCohortRequest request) {
        return ApiResponse.<PeriodCohortResponse>builder()
                .result(periodCohortService.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PeriodCohortResponse>> getAll() {
        return ApiResponse.<List<PeriodCohortResponse>>builder()
                .result(periodCohortService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PeriodCohortResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<PeriodCohortResponse>builder()
                .result(periodCohortService.getById(id))
                .build();
    }

    @GetMapping("/period/{periodId}")
    public ApiResponse<List<PeriodCohortResponse>> getByRegistrationPeriodId(@PathVariable UUID periodId) {
        return ApiResponse.<List<PeriodCohortResponse>>builder()
                .result(periodCohortService.getByRegistrationPeriodId(periodId))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PeriodCohortResponse> update(@PathVariable UUID id, @RequestBody @Valid PeriodCohortUpdateRequest request) {
        return ApiResponse.<PeriodCohortResponse>builder()
                .result(periodCohortService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        periodCohortService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Deleted successfully")
                .build();
    }
}
