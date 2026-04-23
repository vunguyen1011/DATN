package com.example.datn.Pattern.Stragery.scheduling.rule;

import com.example.datn.Model.Lecturer;
import com.example.datn.Model.Schedule;
import com.example.datn.Pattern.Stragery.scheduling.SchedulingContext;
import org.springframework.stereotype.Component;

/**
 * Rule 3 – Gom lịch (Compact Schedule).
 *
 * <p>Ưu tiên GV đã có lịch vào ngày đó để tránh GV phải đến nhiều ngày khác nhau.
 *
 * <ul>
 *   <li>GV đã có lịch vào ngày schedule → +15</li>
 *   <li>Không có lịch ngày đó → 0 (neutral)</li>
 * </ul>
 */
@Component
public class CompactScheduleRule implements ScoreRule {

    @Override
    public int apply(Lecturer lecturer, Schedule schedule, SchedulingContext ctx) {
        if (schedule.getDayOfWeek() == null) return 0;

        boolean hasSameDayClass = ctx.hasLecturerClassOnDay(lecturer.getId(), schedule.getDayOfWeek());
        return hasSameDayClass ? 15 : 0;
    }
}
