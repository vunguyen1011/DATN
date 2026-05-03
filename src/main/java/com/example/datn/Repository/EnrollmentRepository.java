package com.example.datn.Repository;

import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.Model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    Optional<Enrollment> findByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);

    @Query("SELECT COALESCE(SUM(e.classSection.subject.credits), 0) FROM Enrollment e " +
            "WHERE e.student.id = :studentId " +
            "AND e.classSection.semester.id = :semesterId " +
            "AND e.status = :status")
    int sumCreditsByStudentAndSemester(@Param("studentId") UUID studentId, @Param("semesterId") UUID semesterId, @Param("status") EnrollmentStatus status);

    @Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
            "WHERE e.student.id = :studentId " +
            "AND e.classSection.subject.id = :subjectId " +
            "AND e.classSection.semester.id = :semesterId " +
            "AND e.status = :status")
    boolean existsByStudentAndSubjectAndSemester(@Param("studentId") UUID studentId, @Param("subjectId") UUID subjectId, @Param("semesterId") UUID semesterId, @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId " +
            "AND e.classSection.semester.id = :semesterId " +
            "AND e.status = :status")
    List<Enrollment> findActiveEnrollmentsBySemester(@Param("studentId") UUID studentId, @Param("semesterId") UUID semesterId, @Param("status") EnrollmentStatus status);
}