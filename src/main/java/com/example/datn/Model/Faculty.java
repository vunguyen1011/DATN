package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "faculties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // VD: Khoa Công nghệ thông tin

    @Column(nullable = false, unique = true)
    private String code; // VD: CNTT
    @CreationTimestamp
    @Column(name = "established_at", updatable = false)
    private LocalDateTime establishedAt;
}