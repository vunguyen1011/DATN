package com.example.datn.Model;

import com.example.datn.ENUM.ComponentType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "subject_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ComponentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_room_type_id")
    private RoomType requiredRoomType;

    @Column(name = "sessions_per_week")
    private Integer sessionsPerWeek;

    @Column(name = "periods_per_session")
    private Integer periodsPerSession;

    @Column(name = "total_periods")
    private Integer totalPeriods;

    @Column(name = "weight_percent")
    private Double weightPercent;

    @Column(name = "number_credit")
    private Integer numberCredit;
}