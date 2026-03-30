package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "program_subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;



    // Thuộc về đoạn màu cam nào (Bắt buộc hay Tự chọn của nhóm nào)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SubjectGroupSection section;

    // Là môn học nào trong danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;


    @Column(name = "semester")
    private Integer semester; // Học kỳ gợi ý (VD: Kỳ 1, Kỳ 2...)

    @Column(name = "weight")
    private Double weight=1.0; // Trọng số môn học (VD: 1.0, 0.5...)

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}