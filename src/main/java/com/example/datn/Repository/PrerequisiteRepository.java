package com.example.datn.Repository;

import com.example.datn.Model.Prerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrerequisiteRepository extends JpaRepository<Prerequisite, UUID> {

    // 1. Tìm tất cả các điều kiện tiên quyết CỦA một môn học cụ thể
    // Dùng cho API: GET /api/subjects/{id}/prerequisites
    List<Prerequisite> findBySubjectId(UUID subjectId);

    // 2. Xóa tất cả điều kiện tiên quyết cũ trước khi cập nhật mới
    @Modifying
    @Transactional
    void deleteBySubjectId(UUID subjectId);

    // 3. BỔ SUNG: Kiểm tra xem một cặp (Môn học - Môn tiên quyết) đã tồn tại chưa
    // Cực kỳ quan trọng để kiểm tra vòng lặp (Circular Dependency)
    // Ví dụ: Kiểm tra xem môn B có đang là tiên quyết của môn A không
    boolean existsBySubjectIdAndPrerequisiteSubjectId(UUID subjectId, UUID prerequisiteSubjectId);

    // 4. BỔ SUNG: Tìm tất cả các môn học mà môn này ĐANG làm tiên quyết cho chúng
    // Dùng để kiểm tra xem nếu xóa môn này thì ảnh hưởng đến những môn nào
    List<Prerequisite> findByPrerequisiteSubjectId(UUID prerequisiteSubjectId);
}