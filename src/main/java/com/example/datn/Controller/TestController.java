package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Service.Impl.SupportService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Testing / Stress Tests", description = "Các API hỗ trợ kiểm thử tải và tạo dữ liệu ảo")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    private final SupportService supportService;
    @Operation(summary = "API chào mừng kiểm tra kết nối")
    @GetMapping("/hello")
    public String quickTest() {
        return "Server is running!";
    }
    @Operation(summary = "Tạo danh sách Token CSV của các sinh viên dùng cho kiểm thử JMeter")
    @PostMapping("/create-token-csv/{count}")
    public ResponseEntity<Void> createTokenCsv(@PathVariable int count) {
        supportService.createTokenCsvFile(count);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Tạo hàng loạt dữ liệu giả lập phục vụ stress test")
    @PostMapping("/generate-stress-data")
    public ResponseEntity<String> generateStressData() {
        supportService.generateStressTestData();
        return ResponseEntity.ok("Tạo dữ liệu stress test (30 phòng, 40 giảng viên, 600 lớp học phần) thành công!");
    }
}
