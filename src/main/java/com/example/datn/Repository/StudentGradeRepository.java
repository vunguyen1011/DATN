package com.example.datn.Repository;

import com.example.datn.Model.StudentGrade;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, UUID> {

    Optional<StudentGrade> findByEnrollmentId(UUID enrollmentId);

    /**
     * Lấy toàn bộ bảng điểm của sinh viên (kèm thông tin môn học & kỳ học).
     * Dùng cho API xem bảng điểm.
     */
    @Query("SELECT sg FROM StudentGrade sg " +
           "JOIN FETCH sg.enrollment e " +
           "JOIN FETCH e.classSection cs " +
           "JOIN FETCH cs.subject sub " +
           "JOIN FETCH cs.semester sem " +
           "WHERE e.student.id = :studentId " +
           "ORDER BY sem.startDate ASC")
    List<StudentGrade> findAllByStudentId(@Param("studentId") UUID studentId);

    /**
     * Lấy Set các subjectId mà sinh viên đã PASS.
     * Đây là query quan trọng nhất — dùng cho logic đăng ký học:
     * 1. Lọc bỏ môn đã qua khỏi danh sách đăng ký
     * 2. Kiểm tra điều kiện tiên quyết đã đáp ứng chưa
     */
    @Cacheable(value = "passedSubjects", key = "#studentId")
    @Query("SELECT cs.subject.id FROM StudentGrade sg " +
           "JOIN sg.enrollment e " +
           "JOIN e.classSection cs " +
           "WHERE e.student.id = :studentId AND sg.isPassed = true")
    Set<UUID> findPassedSubjectIdsByStudentId(@Param("studentId") UUID studentId);
    @Query("SELECT COUNT(sg) > 0 FROM StudentGrade sg " +
            "WHERE sg.enrollment.student.id = :studentId " +
            "AND sg.enrollment.classSection.subject.id = :subjectId " +
            "AND sg.isPassed = true")
    boolean existsByStudent_IdAndSubject_IdAndIsPassedTrue(
            @Param("studentId") UUID studentId,
            @Param("subjectId") UUID subjectId);

    @Query("SELECT sg FROM StudentGrade sg WHERE sg.enrollment.classSection.id = :classSectionId")
    List<StudentGrade> findAllByClassSectionId(@Param("classSectionId") UUID classSectionId);
}
