package com.example.datn.Repository;

import com.example.datn.Model.RegistrationPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, UUID> {

    @EntityGraph(attributePaths = {"semester"})
    Page<RegistrationPeriod> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"semester"})
    Optional<RegistrationPeriod> findById(UUID id);

    @EntityGraph(attributePaths = {"semester"})
    Page<RegistrationPeriod> findBySemesterId(UUID semesterId, Pageable pageable);

    @Query("SELECT r FROM RegistrationPeriod r WHERE r.semester.id = :semesterId AND r.isActive = true")
    List<RegistrationPeriod> findActivePeriodsBySemester(@Param("semesterId") UUID semesterId);

    boolean existsBySemesterIdAndName(UUID semesterId, String name);

    boolean existsBySemesterIdAndNameAndIdNot(UUID semesterId, String name, UUID id);

    @Query("SELECT COUNT(r) FROM RegistrationPeriod r WHERE r.semester.id = :semesterId " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    long countOverlappingPeriods(
            @Param("semesterId") UUID semesterId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(r) FROM RegistrationPeriod r WHERE r.semester.id = :semesterId " +
            "AND r.id != :excludeId " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    long countOverlappingPeriodsForUpdate(
            @Param("semesterId") UUID semesterId,
            @Param("excludeId") UUID excludeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}