package com.example.datn.Model;

import com.example.datn.ENUM.RecommendationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendation_history", indexes = {
        @Index(name = "idx_student_semester", columnList = "student_id, semester_id"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "recommendation_json", columnDefinition = "TEXT")
    private String recommendationJson;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RecommendationStatus status = RecommendationStatus.ACTIVE;

    @Column(name = "ai_model", length = 50)
    private String aiModel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
