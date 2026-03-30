package com.example.datn.Repository;

import com.example.datn.Model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, UUID> {

    // Kiểm tra xem Tên học kỳ đã tồn tại TRONG CÙNG 1 NĂM HỌC chưa?
    boolean existsByNameAndAcademicYearId(String name, UUID academicYearId);

    // Tìm kiếm theo tên học kỳ
    List<Semester> findByNameContainingIgnoreCase(String keyword);
}