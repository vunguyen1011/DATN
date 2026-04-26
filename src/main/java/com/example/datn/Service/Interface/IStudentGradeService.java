package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.StudentGradeRequest;
import com.example.datn.DTO.Response.StudentGradeResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IStudentGradeService {

    /**
     * Admin nhập hoặc cập nhật điểm cho một enrollment.
     * Nếu đã tồn tại grade cho enrollment đó thì update, chưa thì tạo mới.
     */
    StudentGradeResponse upsertGrade(StudentGradeRequest request);

    /**
     * Sinh viên xem bảng điểm của chính mình (lấy từ SecurityContext).
     */
    List<StudentGradeResponse> getMyTranscript();

    /**
     * Lấy tập hợp các subjectId mà sinh viên đã PASS.
     * Dùng nội bộ trong logic đăng ký học để:
     * 1. Lọc bỏ môn đã qua khỏi danh sách đăng ký
     * 2. Kiểm tra điều kiện tiên quyết
     */
    Set<UUID> getPassedSubjectIds(UUID studentId);
}
