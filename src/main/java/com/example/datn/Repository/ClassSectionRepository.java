package com.example.datn.Repository;

import com.example.datn.Model.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, UUID> {
    
    int countBySemesterIdAndSubjectComponent_SubjectIdAndParentSectionIsNull(UUID semesterId, UUID subjectId);

    boolean existsBySectionCode(String sectionCode);
}
