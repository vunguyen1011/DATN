package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(
    name = "schedules",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_room_timeslot",
        columnNames = {"room_id", "day_of_week", "start_period", "end_period"}
    )
)
@SQLRestriction("is_deleted = false")  // Tự động filter soft-deleted records trong mọi query
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id")
    private Lecturer lecturer;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;    // 2 -> 8 (Thứ 2 đến Chủ nhật)

    @Column(name = "start_period")
    private Integer startPeriod;  // Tiết bắt đầu

    @Column(name = "end_period")
    private Integer endPeriod;    // Tiết kết thúc

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // Soft delete — không xóa vật lý
}
