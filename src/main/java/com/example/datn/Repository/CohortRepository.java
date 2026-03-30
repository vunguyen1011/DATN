package com.example.datn.Repository;

import com.example.datn.Model.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CohortRepository extends JpaRepository<Cohort, UUID> {

    boolean existsByName(String name);
    List<Cohort> findByNameContainingIgnoreCase(String keyword);
}