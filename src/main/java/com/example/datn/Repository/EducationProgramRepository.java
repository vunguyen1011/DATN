package com.example.datn.Repository;

import com.example.datn.Model.EducationProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EducationProgramRepository extends JpaRepository<EducationProgram, UUID> {
    // Lấy danh sách các chương trình đang hoạt động
    List<EducationProgram> findAllByIsActiveTrue();

    // Tìm chương trình theo Ngành học
    List<EducationProgram> findByMajorIdAndIsActiveTrue(UUID majorId);
    Optional<EducationProgram> findByIdAndIsActiveTrue(UUID id);

     boolean existsByName(String name);
     boolean existsByCode(String code);
     List<EducationProgram>findByNameContainingIgnoreCaseAndIsActiveTrue(String keyword);
    @Query("SELECT e FROM EducationProgram e WHERE e.isActive = true AND " +
            "(LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<EducationProgram> searchByNameOrCode(@Param("keyword") String keyword);
}