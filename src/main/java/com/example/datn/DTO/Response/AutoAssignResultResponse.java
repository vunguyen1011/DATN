package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Kết quả trả về sau khi chạy auto-assign toàn bộ lịch học.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoAssignResultResponse {

    /** Số schedule được xếp phòng+giờ+GV thành công */
    private int placed;

    /** Số schedule thất bại (không tìm được slot) */
    private int failed;

    /** Tỷ lệ thành công (%) */
    private double successRate;

    /** Danh sách các schedule thất bại kèm lý do */
    private List<FailedScheduleInfo> failedSchedules;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FailedScheduleInfo {
        private UUID   scheduleId;
        private String sectionCode;
        private String subjectName;
        /** Lý do thất bại: "NO_ROOM", "NO_LECTURER", "CAPACITY_EXCEEDED" */
        private String reason;
    }
}
