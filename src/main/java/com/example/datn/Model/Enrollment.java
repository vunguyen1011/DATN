package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "enrollments", indexes = {
        @Index(name = "idx_enrollment_student_status", columnList = "student_id, status"),
        @Index(name = "idx_enrollment_class_section", columnList = "class_section_id"),
        @Index(name = "idx_enrollment_student_section", columnList = "student_id, class_section_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;
    @Column(name = "enrollment_date")
    private java.time.LocalDateTime enrollmentDate = java.time.LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private com.example.datn.ENUM.EnrollmentStatus status = com.example.datn.ENUM.EnrollmentStatus.REGISTERED;
}