package com.example.datn.Repository;

import com.example.datn.Model.ProgramSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramSubjectRepository extends JpaRepository<ProgramSubject, UUID> {

    List<ProgramSubject> findBySectionIdAndIsActiveTrue(UUID sectionId);

    Optional<ProgramSubject> findBySectionIdAndSubjectIdAndIsActiveTrue(UUID sectionId, UUID subjectId);

    Optional<ProgramSubject> findBySectionIdAndSubjectId(UUID sectionId, UUID subjectId);

    boolean existsBySubjectIdAndSectionIdAndIsActiveTrue(UUID subjectId, UUID sectionId);

    boolean existsBySubjectIdAndIsActiveTrue(UUID subjectId);

    List<ProgramSubject> findBySection_EducationProgram_IdAndIsActiveTrue(UUID id);

    @Query("SELECT ps FROM ProgramSubject ps " +
            "JOIN FETCH ps.subject " +
            "JOIN FETCH ps.section " +
            "WHERE ps.section.educationProgram.id = :programId " +
            "AND ps.isActive = true")
    List<ProgramSubject> findAllByProgramIdFetchSubject(@Param("programId") UUID programId);

    @Query("SELECT COALESCE(SUM(ps.subject.credits), 0) FROM ProgramSubject ps WHERE ps.section.id = :sectionId AND ps.isActive = true")
    Integer sumCreditsBySectionId(@Param("sectionId") UUID sectionId);

    @Query("SELECT ps FROM ProgramSubject ps " +
            "JOIN FETCH ps.subject " +
            "WHERE ps.section.educationProgram.id = :programId " +
            "AND ps.isActive = true " +
            "ORDER BY ps.semester ASC")
    List<ProgramSubject> findAllByProgramIdFlattened(@Param("programId") UUID programId);

    @Query("SELECT ps FROM ProgramSubject ps " +
            "JOIN FETCH ps.subject " +
            "JOIN ProgramCohort pc ON pc.program.id = ps.section.educationProgram.id " +
            "WHERE ps.section.educationProgram.major.id = :majorId " +
            "AND pc.cohort.id = :cohortId " +
            "AND ps.isActive = true " +
            "ORDER BY ps.semester ASC")
    List<ProgramSubject> findSubjectsByCohortAndMajor(@Param("cohortId") UUID cohortId, @Param("majorId") UUID majorId);

    @Query(value = "SELECT ps FROM ProgramSubject ps " +
            "JOIN FETCH ps.subject " +
            "JOIN ProgramCohort pc ON pc.program.id = ps.section.educationProgram.id " +
            "WHERE ps.section.educationProgram.major.id = :majorId " +
            "AND pc.cohort.id = :cohortId " +
            "AND ps.isActive = true " +
            "AND (:keyword = '' OR LOWER(ps.subject.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(ps.subject.code) LIKE LOWER(CONCAT('%', :keyword, '%')))",
            countQuery = "SELECT count(ps) FROM ProgramSubject ps " +
                    "JOIN ps.subject sub " +
                    "JOIN ProgramCohort pc ON pc.program.id = ps.section.educationProgram.id " +
                    "WHERE ps.section.educationProgram.major.id = :majorId " +
                    "AND pc.cohort.id = :cohortId " +
                    "AND ps.isActive = true " +
                    "AND (:keyword = '' OR LOWER(sub.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sub.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ProgramSubject> searchSubjectsByCohortAndMajorPaginated(
            @Param("cohortId") UUID cohortId,
            @Param("majorId") UUID majorId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT ps FROM ProgramSubject ps " +
            "JOIN FETCH ps.subject " +
            "JOIN ProgramCohort pc ON pc.program.id = ps.section.educationProgram.id " +
            "WHERE ps.section.educationProgram.major.id = :majorId " +
            "AND pc.cohort.id = :cohortId " +
            "AND ps.isActive = true " +
            "AND EXISTS (SELECT 1 FROM ClassSection cs WHERE cs.subject.id = ps.subject.id AND cs.semester.id = :semesterId AND cs.status = com.example.datn.ENUM.SectionStatus.OPENED) " +
            "ORDER BY ps.semester ASC")
    List<ProgramSubject> findOpenedSubjectsByCohortAndMajor(
            @Param("cohortId") UUID cohortId,
            @Param("majorId") UUID majorId,
            @Param("semesterId") UUID semesterId);

    @Query(value = "SELECT ps FROM ProgramSubject ps " +
            "JOIN FETCH ps.subject " +
            "JOIN ProgramCohort pc ON pc.program.id = ps.section.educationProgram.id " +
            "WHERE ps.section.educationProgram.major.id = :majorId " +
            "AND pc.cohort.id = :cohortId " +
            "AND ps.isActive = true " +
            "AND EXISTS (SELECT 1 FROM ClassSection cs WHERE cs.subject.id = ps.subject.id AND cs.semester.id = :semesterId AND cs.status = com.example.datn.ENUM.SectionStatus.OPENED) " +
            "AND (:keyword = '' OR LOWER(ps.subject.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(ps.subject.code) LIKE LOWER(CONCAT('%', :keyword, '%')))",
            countQuery = "SELECT count(ps) FROM ProgramSubject ps " +
                    "JOIN ps.subject sub " +
                    "JOIN ProgramCohort pc ON pc.program.id = ps.section.educationProgram.id " +
                    "WHERE ps.section.educationProgram.major.id = :majorId " +
                    "AND pc.cohort.id = :cohortId " +
                    "AND ps.isActive = true " +
                    "AND EXISTS (SELECT 1 FROM ClassSection cs WHERE cs.subject.id = sub.id AND cs.semester.id = :semesterId AND cs.status = com.example.datn.ENUM.SectionStatus.OPENED) " +
                    "AND (:keyword = '' OR LOWER(sub.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sub.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ProgramSubject> searchOpenedSubjectsPaginated(
            @Param("cohortId") UUID cohortId,
            @Param("majorId") UUID majorId,
            @Param("semesterId") UUID semesterId,
            @Param("keyword") String keyword,
            Pageable pageable);
}