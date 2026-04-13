package com.example.datn.Repository;

import com.example.datn.ENUM.ComponentType;
import com.example.datn.Model.SubjectComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectComponentRepository extends JpaRepository<SubjectComponent, UUID> {
    List<SubjectComponent> findBySubjectId(UUID subjectId);
    List<SubjectComponent> findBySubjectIdIn(List<UUID> subjectIds);
    // Kiểm tra xem môn học này đã có loại thành phần (VD: THEORY) này chưa?
    boolean existsBySubjectIdAndType(UUID subjectId, ComponentType type);

    // Dùng cho hàm Update: Kiểm tra trùng loại thành phần nhưng bỏ qua chính bản ghi đang sửa
    boolean existsBySubjectIdAndTypeAndIdNot(UUID subjectId, ComponentType type, UUID id);
}
