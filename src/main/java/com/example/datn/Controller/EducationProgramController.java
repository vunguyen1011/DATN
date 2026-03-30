package com.example.datn.Controller;

import com.example.datn.DTO.Request.EducationProgramRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.EducationProgramResponse;
import com.example.datn.DTO.Response.ProgramTreeResponse;
import com.example.datn.Service.Interface.IEducationProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/education-programs")
@RequiredArgsConstructor
public class EducationProgramController {

    private final IEducationProgramService programService;

    @PostMapping
    public ApiResponse<EducationProgramResponse> createProgram(@Valid @RequestBody EducationProgramRequest request) {
        return ApiResponse.<EducationProgramResponse>builder()
                .code(1000)
                .message("Tạo chương trình đào tạo thành công")
                .result(programService.createProgram(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<EducationProgramResponse>> getAllActivePrograms(  @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.<List<EducationProgramResponse>>builder()
                .code(1000)
                .message("Lấy danh sách chương trình đào tạo thành công")
                .result(programService.getAllPrograms(keyword))
                .build();
    }
//
//    @GetMapping("/{id}")
//    public ApiResponse<EducationProgramResponse> getProgramById(@PathVariable UUID id) {
//        return ApiResponse.<EducationProgramResponse>builder()
//                .code(1000)
//                .message("Lấy thông tin chương trình đào tạo thành công")
//                .result(programService.getProgramById(id))
//                .build();
//    }
//
//    @PutMapping("/{id}")
//    public ApiResponse<EducationProgramResponse> updateProgram(
//            @PathVariable UUID id,
//            @Valid @RequestBody EducationProgramRequest request) {
//        return ApiResponse.<EducationProgramResponse>builder()
//                .code(1000)
//                .message("Cập nhật chương trình đào tạo thành công")
//                .result(programService.updateProgram(id, request))
//                .build();
//    }
//
//    @DeleteMapping("/{id}")
//    public ApiResponse<Void> softDeleteProgram(@PathVariable UUID id) {
//        programService.softDeleteProgram(id);
//        return ApiResponse.<Void>builder()
//                .code(1000)
//                .message("Xóa chương trình đào tạo thành công")
//                .build();
//    }
    @GetMapping("/{id}/tree")
    public ApiResponse<ProgramTreeResponse> getProgramTree(@PathVariable UUID id) {
        return ApiResponse.<ProgramTreeResponse>builder()
                .code(1000)
                .message("Lấy cấu trúc cây chương trình đào tạo thành công")
                .result(programService.getProgramTree(id))
                .build();
    }
}