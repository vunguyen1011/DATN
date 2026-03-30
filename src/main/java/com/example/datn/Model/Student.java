package com.example.datn.Model;

import com.example.datn.ENUM.Gender;
import com.example.datn.ENUM.StudentStatus;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "students")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "student_code", nullable = false, unique = true)
    private String studentCode;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    private String phone;
    @Column(columnDefinition = "TEXT")
    private String address;
    // Trong Student.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort; // Thay cho Integer enrollmentYear
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StudentStatus status = StudentStatus.STUDYING;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_class_id", nullable = false)
    private AdminClass adminClass;
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;


}