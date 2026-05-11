package com.example.datn.Repository;

import com.example.datn.Model.PeriodCohort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PeriodCohortRepository extends JpaRepository<PeriodCohort, UUID> {

    // Hàm này kết hợp JOIN FETCH của bạn và logic cho phép khóa (cohort) là NULL
    @Query("SELECT pc FROM PeriodCohort pc " +
            "JOIN FETCH pc.registrationPeriod rp " +
            "JOIN FETCH rp.semester s " +
            "WHERE (pc.cohort.id = :cohortId OR pc.cohort IS NULL) " +
            "AND rp.isActive = true " +
            "AND pc.startTime <= :currentTime AND pc.endTime >= :currentTime")
    Optional<PeriodCohort> findOngoingCohortPeriod(
            @Param("cohortId") UUID cohortId,
            @Param("currentTime") LocalDateTime currentTime);

    List<PeriodCohort> findByRegistrationPeriodId(UUID registrationPeriodId);
    @Query("SELECT COUNT(p) > 0 FROM PeriodCohort p WHERE " +
            "(p.cohort.id = :cohortId OR (p.cohort IS NULL AND :cohortId IS NULL)) AND " +
            "p.startTime < :endTime AND p.endTime > :startTime AND " +
            "(:excludeId IS NULL OR p.id != :excludeId)")
    boolean existsOverlappingPeriod(
            @Param("cohortId") UUID cohortId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") UUID excludeId);
//    boolean existsBy
}