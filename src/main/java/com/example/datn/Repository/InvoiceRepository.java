package com.example.datn.Repository;

import com.example.datn.ENUM.InvoiceStatus;
import com.example.datn.Model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByStudentId(UUID studentId);
    List<Invoice> findBySemesterId(UUID semesterId);
    Optional<Invoice> findByStudentIdAndSemesterId(UUID studentId, UUID semesterId);
    @Query("SELECT i FROM Invoice i WHERE i.student.id = :studentId " +
            "AND (:status IS NULL OR i.status = :status)")
    Page<Invoice> findByStudentIdAndStatusWithPagination(
            @Param("studentId") UUID studentId,
            @Param("status") InvoiceStatus status,
            Pageable pageable);
    // DÀNH CHO ADMIN: Lấy tất cả, lọc theo Trạng thái và Học kỳ
    @Query("SELECT i FROM Invoice i WHERE " +
            "(:status IS NULL OR i.status = :status) AND " +
            "(:semesterId IS NULL OR i.semester.id = :semesterId)")
    Page<Invoice> findAllForAdminWithFilters(
            @Param("status") InvoiceStatus status,
            @Param("semesterId") UUID semesterId,
            Pageable pageable);

}
