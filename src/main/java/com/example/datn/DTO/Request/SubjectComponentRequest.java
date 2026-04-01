package com.example.datn.DTO.Request;

import com.example.datn.ENUM.ComponentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectComponentRequest {

    @NotNull(message = "ID môn học không được để trống")
    private UUID subjectId;

    @NotNull(message = "Loại thành phần không được để trống")
    private ComponentType type;

    private UUID requiredRoomTypeId;

    @Min(value = 1, message = "Số buổi trên tuần phải lớn hơn 0")
    private Integer sessionsPerWeek;

    @Min(value = 1, message = "Số tiết trên buổi phải lớn hơn 0")
    private Integer periodsPerSession;

    @Min(value = 1, message = "Tổng số tiết phải lớn hơn 0")
    private Integer totalPeriods;

    @Min(value = 0, message = "Trọng số phần trăm không được nhỏ hơn 0")
    private Double weightPercent;

    @Min(value = 1, message = "Số tín chỉ phải lớn hơn 0")
    private Integer numberCredit;
}
