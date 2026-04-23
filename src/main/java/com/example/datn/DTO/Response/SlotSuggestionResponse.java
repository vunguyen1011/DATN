package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Phase 4 – Gợi ý slot trống cho schedule thất bại.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotSuggestionResponse {

    private UUID    roomId;
    private String  roomName;
    private Integer roomCapacity;

    /** 2 = Thứ 2, ..., 8 = Chủ nhật */
    private Integer dayOfWeek;
    private String  dayOfWeekName;

    private Integer startPeriod;
    private Integer endPeriod;

    /** Tổng điểm (cao hơn = tốt hơn) */
    private int     score;

    /** HIGH / MEDIUM / LOW */
    private String  confidence;

    /** Danh sách lý do dễ đọc cho frontend */
    private List<String> reasons;
}
