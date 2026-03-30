package com.example.datn.Repository;

import com.example.datn.Model.AdminClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminClassRepository extends JpaRepository<AdminClass, UUID> {
    boolean existsByName(String name);

    @Query("SELECT a FROM AdminClass a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<AdminClass> searchByName(@Param("keyword") String keyword);
}