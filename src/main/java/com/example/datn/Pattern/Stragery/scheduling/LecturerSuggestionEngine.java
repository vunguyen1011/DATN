package com.example.datn.Pattern.Stragery.scheduling;

import com.example.datn.DTO.Response.LecturerSuggestionResponse;
import com.example.datn.Model.Lecturer;
import com.example.datn.Model.Schedule;
import com.example.datn.Repository.LecturerRepository;
import com.example.datn.Pattern.Stragery.scheduling.rule.ScoreRule; // Import interface ScoreRule của bạn
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Phase 3 – Lecturer Suggestion Engine.
 *
 * Cỗ máy này tự động nhặt tất cả các ScoreRule (Chuyên môn, Cân bằng tải, Gom lịch)
 * để chấm điểm từng giảng viên và trả về Top 5 người phù hợp nhất cho lớp học.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LecturerSuggestionEngine {

    private final LecturerRepository lecturerRepository;
    private final SchedulingMatrixBuilder matrixBuilder;

    // 🔥 MAGIC CỦA SPRING BOOT:
    // Tự động gom tất cả các class implements ScoreRule (đã gắn @Component) vào List này!
    private final List<ScoreRule> scoreRules;

    public List<LecturerSuggestionResponse> suggest(Schedule schedule) {
        UUID semesterId = schedule.getClassSection().getSemester().getId();

        // 1. Build RAM Matrix để check lịch bận của giảng viên cực nhanh (O(1))
        SchedulingContext ctx = matrixBuilder.build(semesterId);

        int day = schedule.getDayOfWeek();
        int start = schedule.getStartPeriod();
        int end = schedule.getEndPeriod();

        List<Lecturer> allLecturers = lecturerRepository.findAll();
        List<LecturerSuggestionResponse> suggestions = new ArrayList<>();

        for (Lecturer lecturer : allLecturers) {
            // [HARD CONSTRAINT]: Giảng viên đang bận dạy lớp khác vào giờ này -> Loại luôn!
            if (isLecturerBusy(ctx, lecturer.getId(), day, start, end)) {
                continue;
            }

            // [SOFT CONSTRAINT]: Chạy qua Hội đồng Giám khảo (Các Rules) để chấm điểm
            int totalScore = 0;
            for (ScoreRule rule : scoreRules) {
                totalScore += rule.apply(lecturer, schedule, ctx);
            }

            // Nếu điểm âm (ví dụ: Sai chuyên môn bị trừ -1000 điểm) -> Loại ngay lập tức!
            if (totalScore < 0) continue;

            // Đóng gói kết quả
            suggestions.add(LecturerSuggestionResponse.builder()
                    .lecturerId(lecturer.getId())
                    .lecturerCode(lecturer.getLecturerCode())
                    .fullName(lecturer.getFullName())
                    .totalScore(totalScore)
                    // (Bạn có thể custom thêm rule để trả về chuỗi reason chi tiết hơn)
                    .matchReason("Điểm độ phù hợp: " + totalScore)
                    .build());
        }

        // Sắp xếp điểm từ cao xuống thấp
        suggestions.sort(Comparator.comparingInt(LecturerSuggestionResponse::getTotalScore).reversed());

        log.info("[Phase 3] Tìm được {} giảng viên phù hợp cho schedule {}", suggestions.size(), schedule.getId());

        // Trả về Top 5 người giỏi/phù hợp nhất để UI không bị quá dài
        return suggestions.stream().limit(5).collect(Collectors.toList());
    }

    private boolean isLecturerBusy(SchedulingContext ctx, UUID lecturerId, int day, int start, int end) {
        for (int p = start; p <= end; p++) {
            if (ctx.isLecturerBusy(lecturerId, day, p)) return true;
        }
        return false;
    }
}