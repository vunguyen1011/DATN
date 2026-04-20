package com.example.datn.Controller;

import com.example.datn.DTO.Request.ScheduleLecturerRequest;
import com.example.datn.DTO.Request.ScheduleRoomRequest;
import com.example.datn.DTO.Response.ApiResponse;
import com.example.datn.DTO.Response.ScheduleInitResponse;
import com.example.datn.DTO.Response.ScheduleResponse;
import com.example.datn.Service.Interface.IScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

        private final IScheduleService scheduleService;

        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<ScheduleInitResponse> createSchedule() {
                return ApiResponse.<ScheduleInitResponse>builder()
                                .code(1000)
                                .message("Khởi tạo lịch học thành công")
                                .result(scheduleService.createSchedule())
                                .build();
        }



        @PatchMapping("/{id}/time")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<ScheduleResponse> assignTime(
                        @PathVariable UUID id,
                        @Valid @RequestBody com.example.datn.DTO.Request.ScheduleTimeRequest request) {
                return ApiResponse.<ScheduleResponse>builder()
                                .code(1000)
                                .message("Xếp thời gian thành công")
                                .result(scheduleService.assignTime(id, request))
                                .build();
        }

        @PatchMapping("/{id}/room")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<ScheduleResponse> assignRoom(
                        @PathVariable UUID id,
                        @Valid @RequestBody ScheduleRoomRequest request) {
                return ApiResponse.<ScheduleResponse>builder()
                                .code(1000)
                                .message("Xếp phòng học thành công")
                                .result(scheduleService.assignRoom(id, request))
                                .build();
        }

        @PatchMapping("/{id}/lecturer")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
        public ApiResponse<ScheduleResponse> assignLecturer(
                        @PathVariable UUID id,
                        @RequestBody ScheduleLecturerRequest request) {
                return ApiResponse.<ScheduleResponse>builder()
                                .code(1000)
                                .message("Phân công giảng viên thành công")
                                .result(scheduleService.assignLecturer(id, request))
                                .build();
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<Void> deleteSchedule(@PathVariable UUID id) {
                scheduleService.deleteSchedule(id);
                return ApiResponse.<Void>builder()
                                .code(1000)
                                .message("Xóa lịch học thành công")
                                .build();
        }

        /**
         * [ADMIN] Xem toàn bộ lịch trong một học kỳ.
         * GET /api/schedules/semester/{semesterId}
         */
        @GetMapping("/semester/{semesterId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<List<ScheduleResponse>> getSchedulesBySemester(
                        @PathVariable UUID semesterId) {
                return ApiResponse.<List<ScheduleResponse>>builder()
                                .code(1000)
                                .message("Lấy danh sách lịch học trong học kỳ thành công")
                                .result(scheduleService.getSchedulesBySemester(semesterId))
                                .build();
        }

        // ── HOD: Quản lý môn học (Xếp giảng viên) ────────────────────────────────

        /**
         * [HOD] Lấy danh sách lịch học chờ phân công giảng viên
         * Bộ môn được tự động determine dựa trên User đang login.
         * GET /api/schedules/hod/pending-lecturers?semesterId=...
         */
        @GetMapping("/hod/pending-lecturers")
        @PreAuthorize("hasRole('HOD')")
        public ApiResponse<List<ScheduleResponse>> getPendingSchedulesForHOD(
                        @RequestParam UUID semesterId) {
                String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                                .getAuthentication().getName();
                return ApiResponse.<List<ScheduleResponse>>builder()
                                .code(1000)
                                .message("Lấy danh sách môn học của Khoa thành công")
                                .result(scheduleService.getPendingSchedulesForHOD(username, semesterId))
                                .build();
        }

        // ── GIẢNG VIÊN: Xem lịch dạy ─────────────────────────────────────────────

        /**
         * [LECTURER] Xem lịch dạy của giảng viên.
         * GET /api/schedules/lecturer/{lecturerId}?semesterId=...
         */
        @GetMapping("/lecturer/{lecturerId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'LECTURER')")
        public ApiResponse<List<ScheduleResponse>> getSchedulesByLecturer(
                        @PathVariable UUID lecturerId,
                        @RequestParam(required = false) UUID semesterId) {
                return ApiResponse.<List<ScheduleResponse>>builder()
                                .code(1000)
                                .message("Lấy lịch dạy của giảng viên thành công")
                                .result(scheduleService.getSchedulesByLecturer(lecturerId, semesterId))
                                .build();
        }

        // ── SINH VIÊN: Xem thời khóa biểu ────────────────────────────────────────

        /**
         * [STUDENT] Xem thời khóa biểu của lớp học phần đã đăng ký.
         * GET /api/schedules/class-section/{classSectionId}
         */
        @GetMapping("/class-section/{classSectionId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'LECTURER', 'STUDENT')")
        public ApiResponse<List<ScheduleResponse>> getSchedulesByClassSection(
                        @PathVariable UUID classSectionId) {
                return ApiResponse.<List<ScheduleResponse>>builder()
                                .code(1000)
                                .message("Lấy thời khóa biểu lớp học phần thành công")
                                .result(scheduleService.getSchedulesByClassSection(classSectionId))
                                .build();
        }

        // ── CHUNG ─────────────────────────────────────────────────────────────────

        /**
         * Xem chi tiết một lịch học.
         * GET /api/schedules/{id}
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'LECTURER', 'STUDENT')")
        public ApiResponse<ScheduleResponse> getScheduleById(@PathVariable UUID id) {
                return ApiResponse.<ScheduleResponse>builder()
                                .code(1000)
                                .message("Lấy chi tiết lịch học thành công")
                                .result(scheduleService.getScheduleById(id))
                                .build();
        }
}
