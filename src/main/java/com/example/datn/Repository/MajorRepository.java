package com.example.datn.Repository;

import com.example.datn.Model.Major;
// import com.example.datn.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // <--- SỬA LẠI IMPORT NÀY
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MajorRepository extends JpaRepository<Major, UUID> {

    boolean existsByCode(String code);

    boolean existsByName(String name);

    Optional<Major> findByCode(String code);

    List<Major> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT m FROM Major m WHERE LOWER(m.code) = LOWER(:input) OR LOWER(m.name) LIKE LOWER(CONCAT('%', :input, '%'))")
    List<Major> findByCodeAndNameContainingIgnoreCase(@Param("input") String input);

    Optional<Major> findByIdAndIsActiveTrue(UUID id);


}