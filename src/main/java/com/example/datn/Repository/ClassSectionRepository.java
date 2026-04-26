package com.example.datn.Repository;

import com.example.datn.Model.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import com.example.datn.Model.Subject;

@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, UUID> {

    int countBySemesterIdAndSubjectComponent_SubjectIdAndParentSectionIsNull(UUID semesterId, UUID subjectId);
    long countBySemesterId(UUID semesterId);

    boolean existsBySectionCode(String sectionCode);
    List<ClassSection> findBySemesterId(UUID id);
    List<ClassSection> findBySubjectId(UUID subjectId);
    boolean existsByParentSectionId(UUID parentSectionId);

    @Query("SELECT DISTINCT cs.subject FROM ClassSection cs WHERE cs.semester.id = :semesterId AND cs.subject.isActive = true")
    List<Subject> findDistinctSubjectsBySemesterId(@Param("semesterId") UUID semesterId);

    @Query("SELECT DISTINCT cs.subject FROM ClassSection cs WHERE " +
            "(:semesterId IS NULL OR cs.semester.id = :semesterId) AND cs.subject.isActive = true AND " +
            "(LOWER(cs.subject.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(cs.subject.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    org.springframework.data.domain.Page<Subject> searchOpenedSubjects(
            @Param("semesterId") UUID semesterId,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);

    List<ClassSection> findBySubjectIdAndSemesterId(UUID subjectId, UUID semesterId);

    @Query("""
        SELECT cs FROM ClassSection cs
        WHERE cs.semester.id = :semesterId
          AND NOT EXISTS (
              SELECT 1 FROM Schedule s WHERE s.classSection = cs
          )
    """)
    List<ClassSection> findSectionsWithoutSchedule(@Param("semesterId") UUID semesterId);

    @Query("""
        SELECT COUNT(cs) FROM ClassSection cs
        WHERE cs.semester.id = :semesterId
          AND NOT EXISTS (
              SELECT 1 FROM Schedule s WHERE s.classSection = cs
          )
    """)
    long countSectionsWithoutSchedule(@Param("semesterId") UUID semesterId);

    // ĐÃ SỬA LỖI CONCAT LIKE
    @Query("SELECT cs FROM ClassSection cs JOIN cs.subject s " +
            "WHERE s.code LIKE CONCAT('%', :keyword, '%') OR s.name LIKE CONCAT('%', :keyword, '%')")
    List<ClassSection> findBySubjectCodeOrName(@Param("keyword") String keyword);

    // ĐÃ SỬA LỖI ENUM & THÊM CƠ CHẾ ĐỒNG BỘ RAM
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ClassSection cs SET cs.status = :openedStatus " +
            "WHERE cs.semester.id = :semesterId AND cs.status = :pendingStatus")
    int approveAllPendingBySemester(
            @Param("semesterId") UUID semesterId,
            @Param("openedStatus") com.example.datn.ENUM.SectionStatus openedStatus,
            @Param("pendingStatus") com.example.datn.ENUM.SectionStatus pendingStatus
    );
}