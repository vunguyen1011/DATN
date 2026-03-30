package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "program_cohorts")
@Data
public class ProgramCohort {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private EducationProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id")
    private Cohort cohort;

    // ƯU ĐIỂM CỦA CÁCH NÀY LÀ BẠN CÓ THỂ THÊM CÁC TRƯỜNG PHỤ
    // Ví dụ: Ngày bắt đầu áp dụng khung này cho khóa này
    @Column(name = "applied_date")
    private LocalDate appliedDate;

    // Hoặc trạng thái riêng biệt cho khóa này
    @Column(name = "is_active_for_cohort")
    private Boolean isActiveForCohort;
}