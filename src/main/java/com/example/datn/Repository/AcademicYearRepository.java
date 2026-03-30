package com.example.datn.Repository;

import com.example.datn.Model.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    boolean existsByName(String name);

    List<AcademicYear> findByNameContainingIgnoreCase(String keyword);
    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.isCurrent = true")
    void resetAllCurrentToFalse();
}