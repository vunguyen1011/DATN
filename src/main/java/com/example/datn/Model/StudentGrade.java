package com.example.datn.Model;

import jakarta.persistence.*;

import java.util.UUID;

public class StudentGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "midterm_score")
    private Double midtermScore;

    @Column(name = "final_score")
    private Double finalScore;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "is_passed")
    private Boolean isPassed;
}
