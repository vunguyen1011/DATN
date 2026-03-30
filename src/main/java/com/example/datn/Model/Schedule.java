package com.example.datn.Model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "schedules")
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
    private Integer dayOfWeek; // 2 -> 8 (Thứ 2 đến Chủ nhật)
    @Column(name = "start_period")
    private Integer startPeriod; // Tiết bắt đầu
    @Column(name = "end_period")
    private Integer endPeriod; // Tiết kết thúc
}