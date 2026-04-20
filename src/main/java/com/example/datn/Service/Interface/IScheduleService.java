package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.ScheduleLecturerRequest;
import com.example.datn.DTO.Request.ScheduleRoomRequest;
import com.example.datn.DTO.Response.ScheduleInitResponse;
import com.example.datn.DTO.Response.ScheduleResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IScheduleService {

    // ── ADMIN: Quản lý lịch học ───────────────────────────────────────────────

    /**
     * [ADMIN] Bước 1: Tạo lịch học cho tất cả lớp học phần chưa có lịch.
     * Chỉ xác định ClassSection, Room/Giờ hoặc Giảng viên để trống.
     * Trả về thống kê (tổng, số đã có, số mới tạo).
     */
    ScheduleInitResponse createSchedule();



    /**
     * [ADMIN] Bước 2a: Xếp thời gian (Thứ, Tiết) cho lịch học.
     */
    ScheduleResponse assignTime(UUID scheduleId, com.example.datn.DTO.Request.ScheduleTimeRequest request);

    /**
     * [ADMIN] Bước 2b: Xếp phòng học.
     * Dựa vào thời gian đã được lưu từ bước 2a.
     */
    ScheduleResponse assignRoom(UUID scheduleId, ScheduleRoomRequest request);

    /**
     * [ADMIN] Xóa lịch học.
     */
    void deleteSchedule(UUID id);

    /**
     * [ADMIN] Xem toàn bộ lịch học trong một học kỳ.
     */
    List<ScheduleResponse> getSchedulesBySemester(UUID semesterId);

    // ── HOD: Phân công giảng viên ─────────────────────────────────────────────

    /**
     * [HOD] Bước 3: Phân công giảng viên vào lịch học đã có.
     * Chỉ chạm vào: Lecturer.
     * Room/Time do Admin xếp KHÔNG bị ảnh hưởng.
     * lecturerId = null → huỷ phân công giảng viên.
     */
    ScheduleResponse assignLecturer(UUID scheduleId, ScheduleLecturerRequest request);

    /**
     * [HOD] Xem danh sách lịch chờ HOD phân công giảng viên.
     */
    List<ScheduleResponse> getPendingSchedulesForHOD(String username, UUID semesterId);

    // ── GIẢNG VIÊN: Xem lịch dạy ─────────────────────────────────────────────

    /**
     * [LECTURER] Xem lịch dạy của mình trong một học kỳ.
     */
    List<ScheduleResponse> getSchedulesByLecturer(String lecturerCode, UUID semesterId);

    // ── SINH VIÊN: Xem thời khóa biểu ────────────────────────────────────────

    /**
     * [STUDENT] Xem thời khóa biểu của một lớp học phần đã đăng ký.
     */
    List<ScheduleResponse> getSchedulesByClassSection(UUID classSectionId);

    // ── CHUNG ─────────────────────────────────────────────────────────────────

    /**
     * Xem chi tiết một lịch học.
     */
    ScheduleResponse getScheduleById(UUID id);
}
