package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.FinalGradeRequest;
import com.example.datn.DTO.Request.MidtermGradeRequest;
import com.example.datn.DTO.Response.StudentGradeResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IStudentGradeService {

    // ==============================================================================
    // THAO TÁC CẬP NHẬT ĐIỂM
    // ==============================================================================

    StudentGradeResponse updateMidtermScore(UUID enrollmentId, Double midtermScore);

    StudentGradeResponse updateFinalScore(UUID enrollmentId, Double finalScore);

    void updateClassSectionMidtermGrades(UUID classSectionId, List<MidtermGradeRequest> requests);

    void updateClassSectionFinalGrades(UUID classSectionId, List<FinalGradeRequest> requests);

    // ==============================================================================
    // THAO TÁC TRUY VẤN - XEM ĐIỂM
    // ==============================================================================

    /**
     * Sinh viên xem bảng điểm của chính mình (lấy từ SecurityContext).
     * Trả về danh sách điểm phẳng (flat list) của tất cả các môn đã học.
     */
    List<StudentGradeResponse> getMyTranscript();

    /**
     * Lấy tập hợp các subjectId mà sinh viên đã PASS (Qua môn).
     * Dùng nội bộ trong logic đăng ký học để:
     * 1. Lọc bỏ môn đã qua khỏi danh sách đăng ký
     * 2. Kiểm tra điều kiện tiên quyết
     */
    Set<UUID> getPassedSubjectIds(UUID studentId);

    /**
     * Sinh viên xem bảng điểm dạng cây theo chương trình đào tạo.
     * Phân nhóm theo Khối kiến thức (Đại cương, Cơ sở ngành, Chuyên ngành...)
     */
    com.example.datn.DTO.Response.TranscriptTreeResponse getMyTranscriptTree();

    /**
     * Lấy danh sách sinh viên và điểm thành phần của một lớp học phần.
     * (Dành cho giảng viên xem danh sách lớp mà mình phụ trách).
     */
    List<com.example.datn.DTO.Response.ClassSectionStudentGradeResponse> getStudentsGradesByClassSection(UUID classSectionId);
}