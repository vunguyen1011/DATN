package com.example.datn.Controller;

import com.example.datn.DTO.Request.MajorRequest;
// Import class ApiResponse của bạn vào đây
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Model.Major;
import com.example.datn.Service.Interface.IMajorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/majors")
@RequiredArgsConstructor
public class MajorController {
    private final IMajorService majorService;

    @PostMapping
    public ApiResponse<Major> createMajor(@Valid @RequestBody MajorRequest request) {
        return ApiResponse.<Major>builder()
                .code(1000) // 1000 thường là mã code quy ước cho Success
                .message("Tạo ngành học thành công")
                .result(majorService.createMajor(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<Major>> getMajor(@RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.<List<Major>>builder()
                .code(1000)
                .message("Lấy danh sách ngành học thành công")
                .result(majorService.searchMajors(keyword))
                .build();

    }


    @GetMapping("/{id}")
    public ApiResponse<Major> getMajorById(@PathVariable UUID id) {
        return ApiResponse.<Major>builder()
                .code(1000)
                .message("Lấy thông tin chi tiết thành công")
                .result(majorService.getMajorById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<Major> updateMajor(
            @PathVariable UUID id,
            @Valid @RequestBody MajorRequest request) {
        return ApiResponse.<Major>builder()
                .code(1000)
                .message("Cập nhật ngành học thành công")
                .result(majorService.updateMajor(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMajor(@PathVariable UUID id) {
        majorService.deleteMajor(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa ngành học thành công")
                // Xóa xong thì không cần trả result về nữa
                .build();
    }


}