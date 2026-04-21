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

        @DeleteMapping("/{id}/time")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<ScheduleResponse> clearTime(@PathVariable UUID id) {
                return ApiResponse.<ScheduleResponse>builder()
                                .code(1000)
                                .message("Hủy phân công thời gian thành công")
                                .result(scheduleService.clearTime(id))
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

        @DeleteMapping("/{id}/room")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<ScheduleResponse> clearRoom(@PathVariable UUID id) {
                return ApiResponse.<ScheduleResponse>builder()
                                .code(1000)
                                .message("Hủy phân công phòng học thành công")
                                .result(scheduleService.clearRoom(id))
                                .build();
        }

        @PatchMapping("/{id}/lecturer")
        @PreAuthorize("hasAnyRole('ADMIN','HOD')")
        public ApiResponse<ScheduleResponse> assignLecturer(
                        @PathVariable UUID id,
                        @RequestBody ScheduleLecturerRequest request) {
                return ApiResponse.<ScheduleResponse>builder()
                                .code(1000)
                                .message("Phân công giảng viên thành công")
                                .result(scheduleService.assignLecturer(id, request))
                                .build();
        }

        @PatchMapping("/bulk-lecturer")
        @PreAuthorize("hasAnyRole('ADMIN','HOD')")
        public ApiResponse<List<ScheduleResponse>> bulkAssignLecturer(
                        @RequestBody com.example.datn.DTO.Request.BulkScheduleLecturerRequest request) {
                return ApiResponse.<List<ScheduleResponse>>builder()
                                .code(1000)
                                .message("Phân công giảng viên hàng loạt thành công")
                                .result(scheduleService.bulkAssignLecturer(request))
                                .build();
        }

        @DeleteMapping("/{id}/lecturer")
        @PreAuthorize("hasAnyRole('ADMIN','HOD')")
        public ApiResponse<ScheduleResponse> clearLecturer(@PathVariable UUID id) {
                return ApiResponse.<ScheduleResponse>builder()
                                .code(1000)
                                .message("Hủy phân công giảng viên thành công")
                                .result(scheduleService.clearLecturer(id))
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
        public ApiResponse<org.springframework.data.domain.Page<ScheduleResponse>> getSchedulesBySemester(
                        @PathVariable UUID semesterId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
                return ApiResponse.<org.springframework.data.domain.Page<ScheduleResponse>>builder()
                                .code(1000)
                                .message("Lấy danh sách lịch học trong học kỳ thành công")
                                .result(scheduleService.getSchedulesBySemester(semesterId, pageable))
                                .build();
        }

        // ── HOD: Quản lý môn học (Xếp giảng viên) ────────────────────────────────

        @GetMapping("/hod/pending-lecturers")
        @PreAuthorize("hasAnyRole('HOD','ADMIN')")
        public ApiResponse<org.springframework.data.domain.Page<ScheduleResponse>> getPendingSchedulesForHOD(
                        @RequestParam UUID semesterId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
                String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                                .getAuthentication().getName();
                return ApiResponse.<org.springframework.data.domain.Page<ScheduleResponse>>builder()
                                .code(1000)
                                .message("Lấy danh sách môn học của Khoa thành công")
                                .result(scheduleService.getPendingSchedulesForHOD(username, semesterId, pageable))
                                .build();
        }

        // ── GIẢNG VIÊN: Xem lịch dạy ─────────────────────────────────────────────


        @GetMapping("/lecturer/{lecturerCode}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'LECTURER')")
        public ApiResponse<org.springframework.data.domain.Page<ScheduleResponse>> getSchedulesByLecturer(
                        @PathVariable String lecturerCode,
                        @RequestParam(required = false) UUID semesterId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
                return ApiResponse.<org.springframework.data.domain.Page<ScheduleResponse>>builder()
                                .code(1000)
                                .message("Lấy lịch dạy của giảng viên thành công")
                                .result(scheduleService.getSchedulesByLecturer(lecturerCode, semesterId, pageable))
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
