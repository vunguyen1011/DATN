package com.example.datn.Repository;

import com.example.datn.Model.SubjectComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectComponentRepository extends JpaRepository<SubjectComponent, UUID> {
    List<SubjectComponent> findBySubjectId(UUID subjectId);
}
