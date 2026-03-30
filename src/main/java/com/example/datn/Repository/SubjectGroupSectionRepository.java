package com.example.datn.Repository;

import com.example.datn.Model.SubjectGroupSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectGroupSectionRepository extends JpaRepository<SubjectGroupSection, UUID> {
    List<SubjectGroupSection> findByIsActiveTrue();


    List<SubjectGroupSection> findByEducationProgramIdAndIsActiveTrue(UUID educationProgramId);
    @Query("SELECT sgs FROM SubjectGroupSection sgs " +
            "JOIN FETCH sgs.subjectGroup " +
            "WHERE sgs.educationProgram.id = :programId " +
            "AND sgs.isActive = true")
    List<SubjectGroupSection> findAllByEducationProgramIdFetchGroup(@Param("programId") UUID programId);
}