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
        org.springframework.data.domain.Page<Schedule> findBySemesterId(@Param("semesterId") UUID semesterId,
                        org.springframework.data.domain.Pageable pageable);

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
                            WHERE s.lecturer.lecturerCode = :lecturerCode
                              AND (:semesterId IS NULL OR s.classSection.semester.id = :semesterId)
                        """)
        org.springframework.data.domain.Page<Schedule> findByLecturerAndSemester(
                        @Param("lecturerCode") String lecturerCode,
                        @Param("semesterId") UUID semesterId,
                        org.springframework.data.domain.Pageable pageable);

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

        // 2. Lấy các lịch CHƯA CÓ PHÒNG (Để đem đi xếp)
        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId AND s.room IS NULL")
        List<Schedule> findUnassignedSchedules(@Param("semesterId") UUID semesterId);

        /**
         * Load schedules chưa có phòng kèm đầy đủ ClassSection + Subject +
         * SubjectComponent.
         * Dùng cho Phase 2 (GreedySchedulerEngine) để tránh N+1 khi lặp.
         */
        @EntityGraph(attributePaths = {
                        "classSection", "classSection.subject",
                        "classSection.subjectComponent",
                        "classSection.subjectComponent.requiredRoomType",
                        "classSection.semester"
        })
        @Query("SELECT s FROM Schedule s WHERE s.classSection.semester.id = :semesterId AND s.room IS NULL")
        List<Schedule> findUnassignedSchedulesWithSection(@Param("semesterId") UUID semesterId);
        // Trả về số lượng lịch bị trùng giữa lớp định đăng ký và danh sách lớp đã đăng ký
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
}
