package com.example.datn.Repository;

import com.example.datn.Model.ProgramSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}