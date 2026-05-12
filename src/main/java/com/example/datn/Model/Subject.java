package com.example.datn.Model;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name; // VD: Lập trình Java
    @Column(nullable = false)
    private Integer credits; // Số tín chỉ (Quan trọng để nhân tiền học phí)
    @Column(nullable = false)
    private String departmentName; // VD: Công nghệ thông tin (Giúp phân loại môn học theo khoa)
    @Builder.Default
    private Boolean isActive = true;
    private Integer totalPeriods;   // Tổng số tiết (VD: 45 tiết)
    private Double coffee;           // VD: 1 (Hệ số tính học phí, có thể dùng để điều chỉnh giá trị học phí cho môn học)
}