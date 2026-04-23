package com.example.datn.Pattern.Stragery.scheduling.rule;

import com.example.datn.Model.Lecturer;
import com.example.datn.Model.Schedule;
import com.example.datn.Pattern.Stragery.scheduling.SchedulingContext;
import org.springframework.stereotype.Component;

/**
 * Rule 2 – Cân bằng tải giảng viên (Load Balancing).
 *
 * <ul>
 *   <li>currentLoad >= MAX_LOAD → HARD_BLOCK (không vượt giới hạn)</li>
 *   <li>Còn dư tải → (MAX_LOAD - currentLoad) * 2 điểm (ưu tiên GV rảnh hơn)</li>
 * </ul>
 */
@Component
public class LoadBalanceRule implements ScoreRule {

    /** Tối đa số tiết/học kỳ một GV được phân công. Có thể cấu hình qua properties sau. */
    static final int MAX_LOAD = 120;

    @Override
    public int apply(Lecturer lecturer, Schedule schedule, SchedulingContext ctx) {
        int currentLoad = ctx.getLecturerLoad(lecturer.getId());

        // HARD BLOCK: vượt tải
        if (currentLoad >= MAX_LOAD) {
            return HARD_BLOCK;
        }

        // Soft prefer: GV càng rảnh càng ưu tiên
        return (MAX_LOAD - currentLoad) * 2;
    }
}
