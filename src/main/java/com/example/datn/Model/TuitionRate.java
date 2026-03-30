package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tuition_rates") // Tên bảng trong Database
@Data                          // Tự động tạo Getter, Setter, toString, equals, hashCode
@NoArgsConstructor             // Constructor không tham số (bắt buộc cho JPA)
@AllArgsConstructor            // Constructor đầy đủ tham số
@Builder                       // Hỗ trợ tạo object theo style TuitionRate.builder()...build()
public class TuitionRate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @Column(name = "cost_per_credit", nullable = false)
    private BigDecimal costPerCredit;
}