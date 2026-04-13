package com.example.datn.Repository;

import com.example.datn.Model.ProgramSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramSubjectRepository extends JpaRepository<ProgramSubject, UUID> {

    // 1. Lấy danh sách môn học theo ID của đoạn màu cam (SubjectGroupSection)
    List<ProgramSubject> findBySectionIdAndIsActiveTrue(UUID sectionId);



    // 3. Kiểm tra xem một môn học đã tồn tại trong đoạn cam đó chưa (tránh add trùng)
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
    Optional<ProgramSubject> findBySectionIdAndSubjectIdAndIsActiveTrue(UUID sectionId, UUID subjectId);

    // Dành cho Sinh Viên: Lấy toàn bộ môn trong Khung chương trình (dạng phẳng)
    @Query("SELECT ps FROM ProgramSubject ps " +
           "JOIN FETCH ps.subject " +
           "WHERE ps.section.educationProgram.id = :programId " +
           "AND ps.isActive = true " +
           "ORDER BY ps.semester ASC")
    List<ProgramSubject> findAllByProgramIdFlattened(@Param("programId") UUID programId);

    // Lấy môn học theo Khóa và Ngành
    @Query("SELECT ps FROM ProgramSubject ps " +
           "JOIN FETCH ps.subject " +
           "JOIN ProgramCohort pc ON pc.program.id = ps.section.educationProgram.id " +
           "WHERE ps.section.educationProgram.major.id = :majorId " +
           "AND pc.cohort.id = :cohortId " +
           "AND ps.isActive = true " +
           "ORDER BY ps.semester ASC")
    List<ProgramSubject> findSubjectsByCohortAndMajor(@Param("cohortId") UUID cohortId, @Param("majorId") UUID majorId);

    // Gợi ý đăng ký: Lấy môn học thuộc Khóa/Ngành MÀ ĐANG CÓ LỚP MỞ TRONG HỌC KỲ ĐÓ
    @Query("SELECT ps FROM ProgramSubject ps " +
           "JOIN FETCH ps.subject " +
           "JOIN ProgramCohort pc ON pc.program.id = ps.section.educationProgram.id " +
           "WHERE ps.section.educationProgram.major.id = :majorId " +
           "AND pc.cohort.id = :cohortId " +
           "AND ps.isActive = true " +
           "AND EXISTS (SELECT 1 FROM ClassSection cs WHERE cs.subject.id = ps.subject.id AND cs.semester.id = :semesterId) " +
           "ORDER BY ps.semester ASC")
    List<ProgramSubject> findOpenedSubjectsByCohortAndMajor(@Param("cohortId") UUID cohortId, @Param("majorId") UUID majorId, @Param("semesterId") UUID semesterId);
}