package com.example.datn.Controller;

import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.Service.Interface.IRedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/registration")
@RequiredArgsConstructor
@Tag(name = "Admin - Registration Management", description = "Các API quản trị dành cho Admin trước/trong đợt đăng ký")
public class AdminRegistrationController {

    private final IRedisService redisService;

    /**
     * Đồng bộ toàn bộ sĩ số các lớp học phần trong một học kỳ lên Redis.
     *
     * LUỒNG SỬ DỤNG:
     *   - Admin gọi API này TRƯỚC KHI mở đợt đăng ký (ví dụ: trước 10 phút).
     *   - Redis sẽ được nạp đầy đủ số slot còn lại = capacity - enrolledCount.
     *   - Kể từ lúc này, hệ thống dùng Redis làm "trọng tài" đếm slot thay vì DB.
     *
     * KẾT QUẢ KỲ VỌNG (sau khi gọi API này):
     *   - Redis keys dạng: class_slot:{classSectionId} = "50"
     *   - Redis keys dạng: class_students:{classSectionId} = (Set rỗng, đã reset)
     */
    @Operation(
        summary = "Sync sĩ số lên Redis",
        description = "Nạp số slot còn lại của tất cả lớp học phần trong một học kỳ vào Redis. " +
                      "BẮT BUỘC phải gọi API này trước khi mở đợt đăng ký tín chỉ."
    )
    @PostMapping("/sync-redis/{semesterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> syncCapacityToRedis(@PathVariable UUID semesterId) {
        redisService.syncClassCapacityToRedis(semesterId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Đồng bộ sĩ số lên Redis thành công")
                .result("Học kỳ [" + semesterId + "] đã được sync lên Redis. Hệ thống sẵn sàng chịu tải!")
                .build();
    }
}
