package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationPeriodRequest {

    @NotNull(message = "ID Học kỳ không được để trống")
    private UUID semesterId;

    @NotBlank(message = "Tên đợt đăng ký không được để trống")
    private String name;

    private Boolean isActive = true;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endTime;
}
