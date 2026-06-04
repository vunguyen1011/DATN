package com.example.datn.Repository;

import com.example.datn.Model.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId")
        org.springframework.data.domain.Page<Schedule> findBySemesterId(@Param("semesterId") UUID semesterId,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.cache.annotation.Cacheable(value = "classSectionSchedules", key = "#classSectionId")
        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "classSection.subjectComponent", "classSection.subjectComponent.subject", "room", "lecturer" })
        List<Schedule> findByClassSection_Id(UUID classSectionId);

        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        @Query("""
                            SELECT s FROM Schedule s
                            WHERE s.lecturer.lecturerCode = :lecturerCode
                              AND (:semesterId IS NULL OR s.classSection.semester.id = :semesterId)
                        """)
        org.springframework.data.domain.Page<Schedule> findByLecturerAndSemester(
                        @Param("lecturerCode") String lecturerCode,
                        @Param("semesterId") UUID semesterId,
                        org.springframework.data.domain.Pageable pageable);

        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        Optional<Schedule> findById(UUID id);

        long countByClassSection_Semester_Id(UUID semesterId);

        // ── CÁC HÀM QUAN TRỌNG CẦN SỬA ĐỂ CHECK TRÙNG LỊCH ──

        /**
         * Kiểm tra trùng phòng: Sửa < thành <= để bắt trường hợp chéo tiết (VD: 11-12
         * và 12-13)
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            SELECT s FROM Schedule s
                            WHERE s.room.id = :roomId
                              AND s.dayOfWeek = :dayOfWeek
                              AND s.isDeleted = false
                              AND s.startPeriod <= :endPeriod
                              AND s.endPeriod >= :startPeriod
                              AND (:excludeId IS NULL OR s.id != :excludeId)
                        """)
        List<Schedule> findConflictingByRoom(
                        @Param("roomId") UUID roomId,
                        @Param("dayOfWeek") Integer dayOfWeek,
                        @Param("startPeriod") Integer startPeriod,
                        @Param("endPeriod") Integer endPeriod,
                        @Param("excludeId") UUID excludeId);

        /**
         * Kiểm tra trùng giảng viên: Sửa < thành <= để bắt trường hợp chéo tiết
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            SELECT s FROM Schedule s
                            WHERE s.lecturer.id = :lecturerId
                              AND s.dayOfWeek = :dayOfWeek
                              AND s.isDeleted = false
                              AND s.startPeriod <= :endPeriod
                              AND s.endPeriod >= :startPeriod
                              AND (:excludeId IS NULL OR s.id != :excludeId)
                        """)
        List<Schedule> findConflictingByLecturer(
                        @Param("lecturerId") UUID lecturerId,
                        @Param("dayOfWeek") Integer dayOfWeek,
                        @Param("startPeriod") Integer startPeriod,
                        @Param("endPeriod") Integer endPeriod,
                        @Param("excludeId") UUID excludeId);

        // ──────────────────────────────────────────────────

        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        @Query("SELECT s FROM Schedule s " +
                        "WHERE s.classSection.semester.id = :semesterId " +
                        "AND s.classSection.subject.departmentName = :departmentName")
        org.springframework.data.domain.Page<Schedule> findPendingSchedulesByDepartmentAndSemester(
                        @Param("departmentName") String departmentName,
                        @Param("semesterId") UUID semesterId,
                        org.springframework.data.domain.Pageable pageable);

        List<Schedule> findByClassSection_IdIn(List<UUID> classSectionIds);

        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId " +
                        "AND s.classSection.subject.departmentName = :departmentName")
        List<Schedule> findBySemesterIdAndDepartmentName(
                        @Param("semesterId") UUID semesterId,
                        @Param("departmentName") String departmentName);

        @EntityGraph(attributePaths = {
                        "classSection", "classSection.subject", "classSection.subjectComponent",
                        "classSection.subjectComponent.requiredRoomType", "room", "lecturer"
        })
        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId AND s.room IS NOT NULL")
        List<Schedule> findAssignedSchedules(@Param("semesterId") UUID semesterId);

        @EntityGraph(attributePaths = {
                        "classSection", "classSection.subject",
                        "classSection.subjectComponent",
                        "classSection.subjectComponent.requiredRoomType",
                        "classSection.semester"
        })
        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId AND s.room IS NULL")
        List<Schedule> findUnassignedSchedulesWithSection(@Param("semesterId") UUID semesterId);

        @Query("SELECT COUNT(s1) FROM Schedule s1, Schedule s2 " +
                        "WHERE s1.classSection.id = :newSectionId " +
                        "AND s2.classSection.id IN :enrolledSectionIds " +
                        "AND s1.dayOfWeek = s2.dayOfWeek " +
                        "AND s1.startPeriod <= s2.endPeriod " +
                        "AND s1.endPeriod >= s2.startPeriod")
        int countOverlappingSchedules(
                        @Param("newSectionId") UUID newSectionId,
                        @Param("enrolledSectionIds") List<UUID> enrolledSectionIds);

        @Query("SELECT COUNT(s1) > 0 FROM Schedule s1, Schedule s2 " +
                        "WHERE s1.classSection.id = :parentId " +
                        "AND s2.classSection.id = :childId " +
                        "AND s1.dayOfWeek IS NOT NULL AND s2.dayOfWeek IS NOT NULL " +
                        "AND s1.dayOfWeek = s2.dayOfWeek " +
                        "AND s1.startPeriod <= s2.endPeriod " +
                        "AND s1.endPeriod >= s2.startPeriod")
        boolean existsOverlapBetweenParentAndChild(
                        @Param("parentId") UUID parentId,
                        @Param("childId") UUID childId);

        boolean existsByClassSection_IdAndLecturer_User_Username(UUID classSectionId, String username);

        List<Schedule> findByClassSection(UUID id);

        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId AND s.dayOfWeek IS NULL")
        org.springframework.data.domain.Page<Schedule> findUnassignedBySemesterId(@Param("semesterId") UUID semesterId,
                        org.springframework.data.domain.Pageable pageable);

        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        @Query("""
                            SELECT s FROM Schedule s
                            WHERE s.lecturer.lecturerCode = :lecturerCode
                              AND s.classSection.semester.id = :semesterId
                        """)
        List<Schedule> findAllByLecturerAndSemester(
                        @Param("lecturerCode") String lecturerCode,
                        @Param("semesterId") UUID semesterId);
}