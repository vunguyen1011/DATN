package com.example.datn.Model;

import com.example.datn.ENUM.SectionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "class_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "section_code", nullable = false, unique = false)
    private String sectionCode;

    @Column(name = "course_group_code", length = 20)
    private String courseGroupCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_component_id", nullable = false)
    private SubjectComponent subjectComponent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_section_id")
    private ClassSection parentSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "min_students")
    private Integer minStudents;

    @Builder.Default
    @Column(name = "enrolled_count", nullable = false)
    private Integer enrolledCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SectionStatus status = SectionStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}