package com.example.datn.Repository;

import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.Model.Enrollment;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
        Optional<Enrollment> findByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);

        /**
         * Upsert nguyên tử: nếu đã tồn tại (student_id, class_section_id) thì UPDATE, nếu chưa thì INSERT.
         * Tránh DataIntegrityViolationException do unique constraint khi 2 luồng cùng tạo
         * cùng 1 enrollment cho cùng 1 sinh viên + lớp.
         */
        @Modifying
        @Transactional
        @Query(value = """
                INSERT INTO enrollments (id, student_id, class_section_id, status, enrollment_date)
                VALUES (:id, :studentId, :classSectionId, :status, :enrollmentDate)
                ON CONFLICT (student_id, class_section_id)
                DO UPDATE SET status = EXCLUDED.status, enrollment_date = EXCLUDED.enrollment_date
                """, nativeQuery = true)
        void upsertEnrollment(
                @Param("id") UUID id,
                @Param("studentId") UUID studentId,
                @Param("classSectionId") UUID classSectionId,
                @Param("status") String status,
                @Param("enrollmentDate") LocalDateTime enrollmentDate
        );

        @Query("SELECT COALESCE(SUM(e.classSection.subject.credits), 0) FROM Enrollment e " +
                        "WHERE e.student.id = :studentId " +
                        "AND e.classSection.semester.id = :semesterId " +
                        "AND e.status = :status")
        int sumCreditsByStudentAndSemester(@Param("studentId") UUID studentId, @Param("semesterId") UUID semesterId,
                        @Param("status") EnrollmentStatus status);

        @Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
                        "WHERE e.student.id = :studentId " +
                        "AND e.classSection.subject.id = :subjectId " +
                        "AND e.classSection.semester.id = :semesterId " +
                        "AND e.status = :status")
        boolean existsByStudentAndSubjectAndSemester(@Param("studentId") UUID studentId,
                        @Param("subjectId") UUID subjectId, @Param("semesterId") UUID semesterId,
                        @Param("status") EnrollmentStatus status);

        @Query("SELECT e.classSection.id FROM Enrollment e WHERE e.student.id = :studentId " +
                        "AND e.classSection.semester.id = :semesterId " +
                        "AND e.status = :status")
        List<UUID> findEnrolledSectionIdsByStudentAndSemester(@Param("studentId") UUID studentId,
                        @Param("semesterId") UUID semesterId, @Param("status") EnrollmentStatus status);

        @EntityGraph(attributePaths = {"classSection", "classSection.subject", "classSection.parentSection"})
        @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId " +
                        "AND e.classSection.semester.id = :semesterId " +
                        "AND e.status = :status")
        List<Enrollment> findActiveEnrollmentsBySemester(@Param("studentId") UUID studentId,
                        @Param("semesterId") UUID semesterId, @Param("status") EnrollmentStatus status);

        List<Enrollment> findByEnrollmentDateBetweenAndStatus(LocalDateTime startTime, LocalDateTime endTime,
                        EnrollmentStatus status);

        Page<Enrollment> findByClassSection_IdAndStatus(UUID classSectionId, EnrollmentStatus status,
                        Pageable pageable);

        List<Enrollment> findByClassSection_IdAndStatus(UUID classSectionId, EnrollmentStatus status);

}