package com.example.datn.Model;

import com.example.datn.ENUM.ProgramStatus;
import com.example.datn.ENUM.ProgramType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "education_programs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // Thêm Mã CTĐT (Rất cần thiết để Admin tìm kiếm)
    @Column(nullable = false, length = 50, unique = true)
    private String code; // VD: CT-KTPM-K18
    @Column(nullable = false, length = 100)
    private String name; // VD: Kỹ thuật phần mềm K18
    // Thay thế min/max bằng 1 cột tổng số tín chỉ
    @Column(name = "total_credits", nullable = false)
    private Integer totalCredits; // VD: 130

    @Column(name = "duration_years", nullable = false)
    private Float durationYears; // VD: 4.0 năm

    // --- CỜ ĐÁNH DẤU TEMPLATE (Linh hồn của tính năng Clone) ---
    @Builder.Default
    @Column(name = "is_template", nullable = false)
    private Boolean isTemplate = false;

    // --- LIÊN KẾT ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    // --- TRẠNG THÁI & AUDIT ---
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}