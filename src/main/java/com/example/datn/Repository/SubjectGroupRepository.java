package com.example.datn.Repository;

import com.example.datn.Model.SubjectGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectGroupRepository extends JpaRepository<SubjectGroup, UUID> {

    // Lấy tất cả nhóm đang active
    List<SubjectGroup> findByIsActiveTrue();
    List<SubjectGroup> findByIsGlobalTrueAndIsActiveTrue();
    boolean existsByName(String name);
    Optional<SubjectGroup> findByIdAndIsActiveTrue(UUID id);


}