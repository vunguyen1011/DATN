package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "admin_classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminClass {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;
}