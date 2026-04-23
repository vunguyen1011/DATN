package com.example.datn.Pattern.Stragery.scheduling.rule;

import com.example.datn.Model.Lecturer;
import com.example.datn.Model.Schedule;
import com.example.datn.Pattern.Stragery.scheduling.SchedulingContext;

/**
 * Strategy interface cho mỗi tiêu chí chấm điểm giảng viên.
 *
 * <ul>
 *   <li>Trả về âm lớn (≤ -9999) = HARD BLOCK (không chọn GV này)</li>
 *   <li>Trả về điểm dương = SOFT preference (ưu tiên)</li>
 * </ul>
 */
public interface ScoreRule {

    int HARD_BLOCK = -9999;

    /**
     * Tính điểm cho một cặp (giảng viên, schedule).
     *
     * @param lecturer  Giảng viên đang được đánh giá
     * @param schedule  Schedule cần phân công
     * @param ctx       Scheduling context chứa các matrix hiện tại
     * @return điểm (có thể âm)
     */
    int apply(Lecturer lecturer, Schedule schedule, SchedulingContext ctx);
}
