package com.example.datn.Repository;

import com.example.datn.Model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

        // 1. Kiểm tra tồn tại
        boolean existsByCode(String code);

        boolean existsByName(String name);

        boolean existsByIdAndIsActiveTrue(UUID id);

        // 2. Các hàm lấy dữ liệu Active (Dùng cho Xóa mềm)
        Page<Subject> findByIsActiveTrue(Pageable pageable);

        Optional<Subject> findByIdAndIsActiveTrue(UUID id);

        // 3. Tìm kiếm hàng loạt (Batch) cho phần Điều kiện tiên quyết
        // Phải thêm "AndIsActiveTrue" để không cho phép gán môn đã xóa làm tiên quyết
        List<Subject> findAllByIdInAndIsActiveTrue(List<UUID> ids);

        // 4. Tìm kiếm theo keyword (Mã hoặc Tên)
        // Cách 1: Dùng Method Name (Spring tự sinh Query)
        List<Subject> findByIsActiveTrueAndCodeContainingIgnoreCaseOrIsActiveTrueAndNameContainingIgnoreCase(
                        String code, String name);

        // Cách 2: Dùng @Query (Gọn gàng và dễ kiểm soát hơn - KHUYÊN DÙNG)
        @Query("SELECT s FROM Subject s WHERE s.isActive = true AND " +
                        "(LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Subject> searchActiveByCodeOrName(@Param("keyword") String keyword, Pageable pageable);

        // Giữ lại hàm search tổng quát nếu bạn cần dùng cho trang Admin (xem cả môn đã
        // xóa)
        @Query("SELECT s FROM Subject s WHERE LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Subject> searchByCodeOrName(@Param("keyword") String keyword);

        boolean existsByCodeAndIsActiveTrue(String code);

        boolean existsByNameAndIsActiveTrue(String name);

        Optional<Subject> findByCode(String code);
        @Query("SELECT DISTINCT sub FROM Subject sub " +
                "JOIN ClassSection cs ON cs.subject = sub " +
                "JOIN Schedule s ON s.classSection = cs " +
                "WHERE cs.semester.id = :semesterId " +
                "AND sub.departmentName = :departmentName " +
                "AND s.lecturer IS NULL")
        List<Subject> findSubjectsWithPendingLecturers(
                @Param("semesterId") UUID semesterId,
                @Param("departmentName") String departmentName);
}