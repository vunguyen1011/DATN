package com.example.datn.Controller;

import com.example.datn.DTO.Request.LecturerUpdateRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Service.Interface.ILecturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/lecturers")
@RequiredArgsConstructor
public class LecturerController {

    private final ILecturerService lecturerService;
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/profile")
    public ApiResponse<UserProfileResponse.LecturerProfile> updateLecturerProfile(
            @PathVariable UUID id,
            @RequestBody LecturerUpdateRequest request) {
        UserProfileResponse.LecturerProfile response = lecturerService.updateLecturerProfile(id, request);
        return ApiResponse.<UserProfileResponse.LecturerProfile>builder()
                .code(1000)
                .message("Cập nhật thông tin giảng viên thành công")
                .result(response)
                .build();
    }
}
