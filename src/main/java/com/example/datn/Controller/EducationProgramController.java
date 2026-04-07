package com.example.datn.Controller;

import com.example.datn.DTO.Request.EducationProgramRequest;
import com.example.datn.DTO.Request.ProgramCohortRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.EducationProgramResponse;
import com.example.datn.DTO.Response.ProgramCohortResponse;
import com.example.datn.DTO.Response.ProgramTreeResponse;
import com.example.datn.Service.Interface.IEducationProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/education-programs")
@RequiredArgsConstructor
public class EducationProgramController {

    private final IEducationProgramService programService;


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<EducationProgramResponse> createProgram(@Valid @RequestBody EducationProgramRequest request) {
        return ApiResponse.<EducationProgramResponse>builder()
                .code(1000)
                .message("Tạo chương trình đào tạo thành công")
                .result(programService.createProgram(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<EducationProgramResponse>> getAllPrograms(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.<List<EducationProgramResponse>>builder()
                .code(1000)
                .message("Lấy danh sách chương trình đào tạo thành công")
                .result(programService.getAllPrograms(keyword))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<EducationProgramResponse> getProgramById(@PathVariable UUID id) {
        return ApiResponse.<EducationProgramResponse>builder()
                .code(1000)
                .message("Lấy thông tin chương trình đào tạo thành công")
                .result(programService.getProgramById(id))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<EducationProgramResponse> updateProgram(
            @PathVariable UUID id,
            @Valid @RequestBody EducationProgramRequest request) {
        return ApiResponse.<EducationProgramResponse>builder()
                .code(1000)
                .message("Cập nhật chương trình đào tạo thành công")
                .result(programService.updateProgram(id, request))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> softDeleteProgram(@PathVariable UUID id) {
        programService.softDeleteProgram(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa chương trình đào tạo thành công")
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/publish")
    public ApiResponse<EducationProgramResponse> publishProgram(@PathVariable UUID id) {
        return ApiResponse.<EducationProgramResponse>builder()
                .code(1000)
                .message("Khóa (publish) chương trình đào tạo thành công")
                .result(programService.publishProgram(id))
                .build();
    }



    @GetMapping("/{id}/tree")
    public ApiResponse<ProgramTreeResponse> getProgramTree(@PathVariable UUID id) {
        return ApiResponse.<ProgramTreeResponse>builder()
                .code(1000)
                .message("Lấy cấu trúc cây chương trình đào tạo thành công")
                .result(programService.getProgramTree(id))
                .build();
    }



    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign-cohort")
    public ApiResponse<ProgramCohortResponse> assignProgramToCohort(
            @Valid @RequestBody ProgramCohortRequest request) {
        return ApiResponse.<ProgramCohortResponse>builder()
                .code(1000)
                .message("Gắn chương trình đào tạo vào khóa học thành công")
                .result(programService.assignProgramToCohort(request))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{programId}/cohorts/{cohortId}")
    public ApiResponse<Void> removeProgramFromCohort(
            @PathVariable UUID programId,
            @PathVariable UUID cohortId) {
        programService.removeProgramFromCohort(programId, cohortId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Gỡ chương trình đào tạo khỏi khóa học thành công")
                .build();
    }

    @GetMapping("/{programId}/cohorts")
    public ApiResponse<List<ProgramCohortResponse>> getCohortsByProgram(@PathVariable UUID programId) {
        return ApiResponse.<List<ProgramCohortResponse>>builder()
                .code(1000)
                .message("Lấy danh sách khóa học theo chương trình đào tạo thành công")
                .result(programService.getCohortsByProgram(programId))
                .build();
    }
}