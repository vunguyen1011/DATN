package com.example.datn.Repository;

import com.example.datn.ENUM.RecommendationStatus;
import com.example.datn.Model.AiRecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiRecommendationHistoryRepository extends JpaRepository<AiRecommendationHistory, UUID> {

    Optional<AiRecommendationHistory> findFirstByStudentIdAndStatusOrderByCreatedAtDesc(UUID studentId, RecommendationStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE AiRecommendationHistory a SET a.status = :newStatus WHERE a.student.id = :studentId AND a.status = :oldStatus")
    void updateStatusByStudent(@Param("studentId") UUID studentId, @Param("oldStatus") RecommendationStatus oldStatus, @Param("newStatus") RecommendationStatus newStatus);

    @Modifying
    @Transactional
    @Query("DELETE FROM AiRecommendationHistory a WHERE a.status = :status AND a.createdAt < :beforeDate")
    void deleteByStatusAndCreatedAtBefore(@Param("status") RecommendationStatus status, @Param("beforeDate") LocalDateTime beforeDate);
}
