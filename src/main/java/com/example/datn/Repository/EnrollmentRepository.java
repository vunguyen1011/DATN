package com.example.datn.Repository;

import com.example.datn.Model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.classSection cs JOIN FETCH cs.subject WHERE e.student.id = :studentId")
    List<Enrollment> findAllByStudentId(@Param("studentId") UUID studentId);

    Optional<Enrollment> findByStudentIdAndClassSectionId(UUID studentId, UUID classSectionId);
}
