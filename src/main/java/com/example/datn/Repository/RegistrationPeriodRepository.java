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

    // Dùng cho Update: Bỏ qua ID hiện tại khi check trùng tên
    boolean existsBySemesterIdAndNameAndIdNot(UUID semesterId, String name, UUID id);

    // Dùng cho Create: Đếm số đợt trùng thời gian
    @Query("SELECT COUNT(r) FROM RegistrationPeriod r WHERE r.semester.id = :semesterId " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    long countOverlappingPeriods(
            @Param("semesterId") UUID semesterId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Dùng cho Update: Đếm số đợt trùng thời gian (Đã đổi tên và thứ tự tham số cho khớp với Service)
    @Query("SELECT COUNT(r) FROM RegistrationPeriod r WHERE r.semester.id = :semesterId " +
            "AND r.id != :excludeId " +
            "AND r.startTime < :endTime AND r.endTime > :startTime")
    long countOverlappingPeriodsExcludingId(
            @Param("semesterId") UUID semesterId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") UUID excludeId);
}