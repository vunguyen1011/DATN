package com.example.datn.Pattern.Stragery.scheduling.rule;

import com.example.datn.Model.Lecturer;
import com.example.datn.Model.Schedule;
import com.example.datn.Pattern.Stragery.scheduling.SchedulingContext;
import org.springframework.stereotype.Component;

/**
 * Rule 1 – Khớp môn học (Subject Match).
 *
 * <ul>
 *   <li>GV thuộc khoa == subject.departmentName → +30 (primary match)</li>
 *   <li>Không khớp khoa → HARD_BLOCK (không để GV dạy trái khoa)</li>
 * </ul>
 *
 * <b>Lưu ý:</b> Khi hệ thống có bảng lecturer_subjects (danh sách môn GV dạy được),
 * hãy thay đổi logic ở đây thay vì sửa code ở nơi khác.
 */
@Component
public class SubjectMatchRule implements ScoreRule {

    @Override
    public int apply(Lecturer lecturer, Schedule schedule, SchedulingContext ctx) {
        if (schedule.getClassSection() == null || schedule.getClassSection().getSubject() == null) {
            return HARD_BLOCK;
        }

        String subjectDept   = schedule.getClassSection().getSubject().getDepartmentName();
        String lecturerMajor = lecturer.getMajor() != null ? lecturer.getMajor().getName() : null;

        if (subjectDept == null || lecturerMajor == null) {
            return HARD_BLOCK;
        }

        // GV cùng khoa với môn học → ưu tiên cao nhất
        if (subjectDept.equalsIgnoreCase(lecturerMajor)) {
            return 30;
        }

        // Khác khoa → hard block (không xếp lịch trái ngành)
        return HARD_BLOCK;
    }
}
