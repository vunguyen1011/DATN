package com.example.datn.Repository;

import com.example.datn.Model.SectionDefaultSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectionDefaultSubjectRepository extends JpaRepository<SectionDefaultSubject, UUID> {
    List<SectionDefaultSubject> findBySectionDefaultIdAndIsActiveTrue(UUID sectionDefaultId);
    boolean existsBySectionDefaultIdAndSubjectIdAndIsActiveTrue(UUID sectionDefaultId, UUID subjectId);
    boolean  existsBySubjectIdAndIsActiveTrue(UUID subjectId);
    List<SectionDefaultSubject> findBySectionDefaultIdInAndIsActiveTrue(List<UUID> sectionDefaultIds);
    Optional<SectionDefaultSubject>findBySectionDefaultIdAndSubjectIdAndIsActiveTrue(UUID sectionDefaultId, UUID subjectId);
}