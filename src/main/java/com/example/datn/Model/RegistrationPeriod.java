package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "registration_periods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "name", nullable = false)
    private String name; // VD: Đăng ký tín chỉ HK1 2026-2027

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}