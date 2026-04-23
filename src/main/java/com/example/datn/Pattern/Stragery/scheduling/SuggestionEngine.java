package com.example.datn.Pattern.Stragery.scheduling;

import com.example.datn.DTO.Response.SlotSuggestionResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.Room;
import com.example.datn.Model.Schedule;
import com.example.datn.Model.SubjectComponent;
import com.example.datn.Repository.RoomRepository;
import com.example.datn.Repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 4 – Suggestion Engine.
 *
 * <p>Với một schedule thất bại (chưa có phòng/giờ), engine này duyệt tất cả
 * slot còn khả dụng, chấm điểm, và trả về top-N gợi ý kèm lý do.
 *
 * <pre>
 * Slot score =
 *   + roomFitScore       (sức chứa đủ → +20, vừa đủ → +10)
 *   + timeQualityScore   (buổi sáng tiết 1-5 → +10)
 *   - fragmentationPenalty (tạo khoảng trống giữa các tiết → -15)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuggestionEngine {

    private static final int[] WORKING_DAYS = {2, 3, 4, 5, 6, 7};
    private static final int   MAX_PERIOD   = 15;

    private final ScheduleRepository      scheduleRepository;
    private final RoomRepository          roomRepository;
    private final SchedulingMatrixBuilder matrixBuilder;


    public List<SlotSuggestionResponse> suggest(UUID scheduleId, int topN) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        UUID semesterId = schedule.getClassSection().getSemester().getId();

        // Build context từ trạng thái hiện tại của DB
        SchedulingContext ctx = matrixBuilder.build(semesterId);

        SubjectComponent comp     = schedule.getClassSection().getSubjectComponent();
        int periods    = comp != null && comp.getPeriodsPerSession() != null
                         ? comp.getPeriodsPerSession() : 3;
        int capacity   = schedule.getClassSection().getCapacity();
        UUID subjectId = schedule.getClassSection().getSubject().getId();

        // Lọc phòng phù hợp loại
        var reqType = comp != null ? comp.getRequiredRoomType() : null;
        List<Room> candidateRooms = roomRepository.findAll().stream()
                .filter(r -> r.getCapacity() != null && r.getCapacity() >= capacity)
                .filter(r -> reqType == null || reqType.getId().equals(r.getRoomType().getId()))
                .collect(Collectors.toList());

        List<SlotSuggestionResponse> suggestions = new ArrayList<>();

        for (int day : WORKING_DAYS) {
            for (int start = 1; start + periods - 1 <= MAX_PERIOD; start += periods) {
                int end = start + periods - 1;

                // Look-ahead capacity check
                boolean capacityOk = checkCapacity(ctx, subjectId, day, start, end);
                if (!capacityOk) continue;

                for (Room room : candidateRooms) {
                    // Kiểm tra phòng trống
                    boolean roomFree = isRoomFree(ctx, room.getId(), day, start, end);
                    if (!roomFree) continue;

                    // Tính điểm slot
                    List<String> reasons = new ArrayList<>();
                    int score = 0;

                    // 1. Room fit score
                    int surplus = room.getCapacity() - capacity;
                    if (surplus == 0) {
                        score += 20;
                        reasons.add("Phòng vừa đúng sức chứa");
                    } else if (surplus <= 10) {
                        score += 15;
                        reasons.add("Phòng đủ chỗ");
                    } else {
                        score += 10;
                        reasons.add("Phòng rộng hơn cần thiết");
                    }

                    // 2. Time quality score (sáng sớm ưu tiên)
                    if (start <= 5) {
                        score += 10;
                        reasons.add("Giờ dạy sáng (tiết " + start + "-" + end + ")");
                    } else if (start <= 9) {
                        score += 5;
                        reasons.add("Giờ dạy chiều (tiết " + start + "-" + end + ")");
                    }

                    // 3. Fragmentation penalty: nếu slot tạo gap bất thường trong ngày
                    if (createsGap(ctx, room.getId(), day, start, end)) {
                        score -= 15;
                        reasons.add("⚠ Tạo khoảng trống trong lịch phòng");
                    }

                    // 4. Look-ahead capacity bonus
                    int concurrent = ctx.getSubjectConcurrent(subjectId, day, start);
                    int max = ctx.getMaxConcurrentBySubject().getOrDefault(subjectId, 99);
                    if (concurrent < max / 2) {
                        score += 5;
                        reasons.add("Môn học còn nhiều khung giờ trống");
                    }

                    suggestions.add(SlotSuggestionResponse.builder()
                            .roomId(room.getId())
                            .roomName(room.getName())
                            .roomCapacity(room.getCapacity())
                            .dayOfWeek(day)
                            .dayOfWeekName(toDayName(day))
                            .startPeriod(start)
                            .endPeriod(end)
                            .score(score)
                            .confidence(toConfidence(score))
                            .reasons(reasons)
                            .build());
                }
            }
        }

        // Sort by score DESC, lấy top-N
        suggestions.sort(Comparator.comparingInt(SlotSuggestionResponse::getScore).reversed());

        log.info("[Suggestion] Tìm được {} slot khả dụng cho schedule {}", suggestions.size(), scheduleId);

        return suggestions.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isRoomFree(SchedulingContext ctx, UUID roomId,
                               int day, int start, int end) {
        for (int p = start; p <= end; p++) {
            if (ctx.isRoomBusy(roomId, day, p)) return false;
        }
        return true;
    }

    private boolean checkCapacity(SchedulingContext ctx, UUID subjectId,
                                  int day, int start, int end) {
        int max = ctx.getMaxConcurrentBySubject().getOrDefault(subjectId, Integer.MAX_VALUE);
        for (int p = start; p <= end; p++) {
            if (ctx.getSubjectConcurrent(subjectId, day, p) >= max) return false;
        }
        return true;
    }

    /**
     * Kiểm tra xem slot mới có tạo ra "gap" (khoảng trống) giữa các buổi trong ngày không.
     * Ví dụ: phòng có lịch tiết 1-3 và 10-12, thêm tiết 7-9 → không tạo gap.
     * Nhưng nếu phòng có 1-3, thêm 10-12 → có gap → penalty.
     */
    private boolean createsGap(SchedulingContext ctx, UUID roomId, int day, int start, int end) {
        // Kiểm tra xem có tiết nào trước start nhưng không liền kề không
        boolean hasEarlier = false;
        boolean hasGapBefore = false;
        for (int p = 1; p < start; p++) {
            if (ctx.isRoomBusy(roomId, day, p)) hasEarlier = true;
        }
        if (hasEarlier) {
            // Kiểm tra tiết ngay trước start có trống không
            hasGapBefore = !ctx.isRoomBusy(roomId, day, start - 1);
        }
        return hasGapBefore;
    }

    private String toConfidence(int score) {
        if (score >= 30) return "HIGH";
        if (score >= 15) return "MEDIUM";
        return "LOW";
    }

    private String toDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            case 8 -> "Chủ nhật";
            default -> "Không xác định";
        };
    }
}
