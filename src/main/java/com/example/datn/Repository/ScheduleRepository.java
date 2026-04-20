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

        // ── Queries có @EntityGraph — giải quyết N+1 bằng 1 câu SQL JOIN ─────────

        /**
         * Lấy toàn bộ lịch trong một học kỳ (qua ClassSection → Semester).
         * 
         * @EntityGraph kéo luôn các bảng liên quan trong 1 câu JOIN.
         */
        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId")
        List<Schedule> findBySemesterId(@Param("semesterId") UUID semesterId);

        /**
         * Lấy lịch theo lớp học phần — dùng derived query + @EntityGraph.
         */
        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        List<Schedule> findByClassSection_Id(UUID classSectionId);

        /**
         * Lấy lịch dạy của giảng viên. Filter semesterId thẳng ở DB.
         * semesterId = null → lấy tất cả học kỳ.
         */
        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        @Query("""
                            SELECT s FROM Schedule s
                            WHERE s.lecturer.id = :lecturerId
                              AND (:semesterId IS NULL OR s.classSection.semester.id = :semesterId)
                        """)
        List<Schedule> findByLecturerAndSemester(
                        @Param("lecturerId") UUID lecturerId,
                        @Param("semesterId") UUID semesterId);

        /**
         * Xem chi tiết một lịch học với @EntityGraph.
         */
        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        Optional<Schedule> findById(UUID id);

        // ── Đếm & kiểm tra ───────────────────────────────────────────────────────

        /** Đếm số lịch đã tạo trong một học kỳ. */
        long countByClassSection_Semester_Id(UUID semesterId);

        // Lấy tất cả lịch theo phòng học (dùng nội bộ)
        List<Schedule> findByRoom_Id(UUID roomId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            SELECT s FROM Schedule s
                            WHERE s.room.id = :roomId
                              AND s.dayOfWeek = :dayOfWeek
                              AND s.startPeriod < :endPeriod
                              AND :startPeriod < s.endPeriod
                              AND (:excludeId IS NULL OR s.id != :excludeId)
                        """)
        List<Schedule> findConflictingByRoom(
                        @Param("roomId") UUID roomId,
                        @Param("dayOfWeek") Integer dayOfWeek,
                        @Param("startPeriod") Integer startPeriod,
                        @Param("endPeriod") Integer endPeriod,
                        @Param("excludeId") UUID excludeId);

        /**
         * Kiểm tra trùng lịch giảng viên (cùng giảng viên, cùng ngày, tiết giao nhau).
         * 
         * @Lock PESSIMISTIC_WRITE: lock các rows tìm được — transaction khác phải chờ.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            SELECT s FROM Schedule s
                            WHERE s.lecturer.id = :lecturerId
                              AND s.dayOfWeek = :dayOfWeek
                              AND s.startPeriod < :endPeriod
                              AND :startPeriod < s.endPeriod
                              AND (:excludeId IS NULL OR s.id != :excludeId)
                        """)
        List<Schedule> findConflictingByLecturer(
                        @Param("lecturerId") UUID lecturerId,
                        @Param("dayOfWeek") Integer dayOfWeek,
                        @Param("startPeriod") Integer startPeriod,
                        @Param("endPeriod") Integer endPeriod,
                        @Param("excludeId") UUID excludeId);

        /**
         * Lấy danh sách lịch chờ HOD phân công giảng viên.
         * Điều kiện: subject.departmentName == hodDepartmentName, và lecturer == null
         * (hoặc lấy tất do HOD tự điều phối lại).
         * Ở đây lấy tất cả trong kỳ thuộc Department của HOD để HOD quản lý.
         */
        @EntityGraph(attributePaths = { "classSection", "classSection.subject", "room", "lecturer" })
        @Query("SELECT s FROM Schedule s " +
                        "WHERE s.classSection.semester.id = :semesterId " +
                        "AND s.classSection.subject.departmentName = :departmentName")
        List<Schedule> findPendingSchedulesByDepartmentAndSemester(
                        @Param("departmentName") String departmentName,
                        @Param("semesterId") UUID semesterId);

        List<Schedule> findByClassSection_IdIn(List<UUID> classSectionIds);

        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId " +
                        "AND s.classSection.subject.departmentName = :departmentName")
        List<Schedule> findBySemesterIdAndDepartmentName(
                        @Param("semesterId") UUID semesterId,
                        @Param("departmentName") String departmentName);

        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId AND s.room IS NOT NULL")
        List<Schedule> findAssignedSchedules(@Param("semesterId") UUID semesterId);

        // 2. Lấy các lịch CHƯA CÓ PHÒNG (Để đem đi xếp)
        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId AND s.room IS NULL")
        List<Schedule> findUnassignedSchedules(@Param("semesterId") UUID semesterId);
}
