package com.example.datn.Controller;

import com.example.datn.DTO.Request.SubjectGroupRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.SubjectGroupResponse;
import com.example.datn.Service.Interface.ISubjectGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Subject Group", description = "Quản lý nhóm môn học (tự chọn/bắt buộc)")
@RestController
@RequestMapping("/api/subject-groups")
@RequiredArgsConstructor
public class SubjectGroupController {

    private final ISubjectGroupService subjectGroupService;

    @Operation(summary = "Tạo nhóm môn học mới")
    @PostMapping
    public ApiResponse<SubjectGroupResponse> createGroup(@Valid @RequestBody SubjectGroupRequest request) {
        return ApiResponse.<SubjectGroupResponse>builder()
                .code(1000)
                .message("Tạo nhóm môn học thành công")
                .result(subjectGroupService.createGroup(request))
                .build();
    }

    @Operation(summary = "Lấy danh sách tất cả các nhóm môn học")
    @GetMapping
    public ApiResponse<List<SubjectGroupResponse>> getAllGroups() {
        return ApiResponse.<List<SubjectGroupResponse>>builder()
                .code(1000)
                .message("Lấy danh sách nhóm môn học thành công")
                .result(subjectGroupService.getAllActiveGroups())
                .build();
    }

    // API RẤT QUAN TRỌNG: Lấy danh sách nhóm theo ID Chương trình
//    @GetMapping("/program/{programId}")
//    public ApiResponse<List<SubjectGroupResponse>> getGroupsByProgram(@PathVariable UUID programId) {
//        return ApiResponse.<List<SubjectGroupResponse>>builder()
//                .code(1000)
//                .message("Lấy danh sách nhóm theo chương trình thành công")
//                .result(subjectGroupService.getGroupsByProgramId(programId))
//                .build();
//    }

    @Operation(summary = "Lấy chi tiết nhóm môn học theo ID")
    @GetMapping("/{id}")
    public ApiResponse<SubjectGroupResponse> getGroupById(@PathVariable UUID id) {
        return ApiResponse.<SubjectGroupResponse>builder()
                .code(1000)
                .message("Lấy thông tin nhóm môn học thành công")
                .result(subjectGroupService.getGroupById(id))
                .build();
    }

    @Operation(summary = "Cập nhật thông tin nhóm môn học")
    @PutMapping("/{id}")
    public ApiResponse<SubjectGroupResponse> updateGroup(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectGroupRequest request) {
        return ApiResponse.<SubjectGroupResponse>builder()
                .code(1000)
                .message("Cập nhật nhóm môn học thành công")
                .result(subjectGroupService.updateGroup(id, request))
                .build();
    }

    @Operation(summary = "Xóa nhóm môn học")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> softDeleteGroup(@PathVariable UUID id) {
        subjectGroupService.softDeleteGroup(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa nhóm môn học thành công")
                .build();
    }
}