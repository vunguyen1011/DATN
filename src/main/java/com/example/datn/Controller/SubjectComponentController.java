package com.example.datn.Controller;

import com.example.datn.DTO.Request.SubjectComponentRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SubjectComponentResponse;
import com.example.datn.Service.Interface.ISubjectComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subject-components")
@RequiredArgsConstructor
public class SubjectComponentController {

    private final ISubjectComponentService subjectComponentService;

    @PostMapping
    public ApiResponse<SubjectComponentResponse> createSubjectComponent(@RequestBody @Valid SubjectComponentRequest request) {
        return ApiResponse.<SubjectComponentResponse>builder()
                .code(1000)
                .message("Tạo thành phần môn học thành công")
                .result(subjectComponentService.createSubjectComponent(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<SubjectComponentResponse> updateSubjectComponent(
            @PathVariable UUID id,
            @RequestBody @Valid SubjectComponentRequest request) {
        return ApiResponse.<SubjectComponentResponse>builder()
                .code(1000)
                .message("Cập nhật thành phần môn học thành công")
                .result(subjectComponentService.updateSubjectComponent(id, request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SubjectComponentResponse> getSubjectComponentById(@PathVariable UUID id) {
        return ApiResponse.<SubjectComponentResponse>builder()
                .code(1000)
                .message("Lấy thông tin thành phần môn học thành công")
                .result(subjectComponentService.getSubjectComponentById(id))
                .build();
    }

    @GetMapping("/subject/{subjectId}")
    public ApiResponse<List<SubjectComponentResponse>> getComponentsBySubjectId(@PathVariable UUID subjectId) {
        return ApiResponse.<List<SubjectComponentResponse>>builder()
                .code(1000)
                .message("Lấy danh sách thành phần theo môn học thành công")
                .result(subjectComponentService.getComponentsBySubjectId(subjectId))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSubjectComponent(@PathVariable UUID id) {
        subjectComponentService.deleteSubjectComponent(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa thành phần môn học thành công")
                .build();
    }
}
