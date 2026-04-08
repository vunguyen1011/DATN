package com.example.datn.Model;

import com.example.datn.ENUM.Gender;
import com.example.datn.ENUM.LecturerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lecturers") // Tên bảng số nhiều
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lecturer_code", nullable = false, unique = true)
    private String lecturerCode;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false)
    private String fullName;
    private String phone;
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "degree")
    private String degree;
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // (WORKING, RETIRED...)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LecturerStatus status = LecturerStatus.WORKING;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "major", nullable = false)
    private Major major;



}