package com.example.datn.Repository;

import com.example.datn.Model.SectionDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionDefaultRepository extends JpaRepository<SectionDefault, UUID> {
    List<SectionDefault> findBySubjectGroupIdOrderByIndexAsc(UUID subjectGroupId);
    List<SectionDefault> findBySubjectGroupIdAndIsActiveTrue(UUID subjectGroupId);
    boolean existsByTitleAndSubjectGroupId(String title, UUID subjectGroupId);
    List<SectionDefault> findBySubjectGroupIdInAndIsActiveTrue(List<UUID> subjectGroupIds);

}