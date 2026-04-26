package com.example.datn.Controller;

import com.example.datn.DTO.Request.ScheduleLecturerRequest;
import com.example.datn.DTO.Request.ScheduleRoomRequest;
import com.example.datn.DTO.Response.*;
import com.example.datn.Service.Interface.IScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

        private final IScheduleService scheduleService;

        // ── GIAI ĐOẠN 0: KHỞI TẠO LỊCH ───────────────────────────────────────────

        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<ScheduleInitResponse> createSchedule() {
                return ApiResponse.<ScheduleInitResponse>builder()
                        .code(1000)
                        .message("Khởi tạo lịch học thành công")
                        .result(scheduleService.createSchedule())
                        .build();
        }

        // ── GIAI ĐOẠN 2 & 3: AUTO SCHEDULING (MÁY CHẠY) ──────────────────────────

        @PostMapping("/semester/{semesterId}/auto-assign")
        @PreAuthorize("hasAnyRole('ADMIN','DEAN')")
        public ApiResponse<AutoAssignResultResponse> autoAssignRoomAndTime(
                @PathVariable UUID semesterId) {
                return ApiResponse.<AutoAssignResultResponse>builder()
                        .code(1000)
                        .message("Chạy thuật toán xếp Phòng và Giờ tự động thành công")
                        .result(scheduleService.autoAssignRoomAndTime(semesterId))
                        .build();
        }

        @DeleteMapping("/semester/{semesterId}/auto-assign")
        @PreAuthorize("hasAnyRole('ADMIN','DEAN')")
        public ApiResponse<Void> clearSemesterSchedule(@PathVariable UUID semesterId) {
                scheduleService.clearSemesterSchedule(semesterId);
                return ApiResponse.<Void>builder()
                        .code(1000)
                        .message("Xóa kết quả xếp lịch thành công")
                        .build();
        }

        @GetMapping("/{id}/suggest-lecturers")
        @PreAuthorize("hasAnyRole('ADMIN','HOD')")
        public ApiResponse<List<LecturerSuggestionResponse>> suggestLecturers(
                @PathVariable UUID id) {
                return ApiResponse.<List<LecturerSuggestionResponse>>builder()
                        .code(1000)
                        .message("Lấy danh sách gợi ý giảng viên thành công")
                        .result(scheduleService.suggestLecturersForSchedule(id))
                        .build();
        }

        @GetMapping("/{id}/suggest-slots")
        @PreAuthorize("hasRole('ADMIN','DEAN')")
        public ApiResponse<List<SlotSuggestionResponse>> suggestSlots(
                @PathVariable UUID id,
                @RequestParam(defaultValue = "5") int topN) {
                return ApiResponse.<List<SlotSuggestionResponse>>builder()
                        .code(1000)
                        .message("Lấy danh sách gợi ý slot/phòng khả dụng thành công")
                        .result(scheduleService.suggestSlotsForSchedule(id, topN))
                        .build();
        }

        // ── GIAI ĐOẠN 1: MANUAL ASSIGNMENT (XẾP TAY / TINH CHỈNH) ────────────────

        @PatchMapping("/{id}/time")
        @PreAuthorize("hasRole('ADMIN','DEAN')")
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
        @PreAuthorize("hasAnyRole('ADMIN','DEAN')")
        public ApiResponse<ScheduleResponse> clearTime(@PathVariable UUID id) {
                return ApiResponse.<ScheduleResponse>builder()
                        .code(1000)
                        .message("Hủy phân công thời gian thành công")
                        .result(scheduleService.clearTime(id))
                        .build();
        }

        @PatchMapping("/{id}/room")
        @PreAuthorize("hasAnyRole('ADMIN','DEAN')")
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
        @PreAuthorize("hasAnyRole('ADMIN','DEAN')")
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
        @PreAuthorize("hasARole('ADMIN')")
        public ApiResponse<Void> deleteSchedule(@PathVariable UUID id) {
                scheduleService.deleteSchedule(id);
                return ApiResponse.<Void>builder()
                        .code(1000)
                        .message("Xóa lịch học thành công")
                        .build();
        }

        // ── CÁC HÀM GET DỮ LIỆU (VIEWER) ─────────────────────────────────────────

        @GetMapping("/semester/{semesterId}")
        @PreAuthorize("hasAnyRole('ADMIN','DEAN','USER','HOD','LECTURER')")
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

        @GetMapping("/semester/{semesterId}/export-pdf")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'LECTURER', 'USERgỉ ','DEAN')")
        public void exportSemesterScheduleToPdf(
                @PathVariable UUID semesterId,
                jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
                scheduleService.exportSemesterScheduleToPdf(semesterId, response);
        }

        @GetMapping("/hod/pending-lecturers")
        @PreAuthorize("hasAnyRole('HOD','ADMIN','DEAN')")
        public ApiResponse<List<SubjectResponse>> getPendingSchedulesForHOD(
                @RequestParam UUID semesterId,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {

                String username = SecurityContextHolder.getContext()
                        .getAuthentication().getName();
                return ApiResponse.<List<SubjectResponse>>builder()
                        .code(1000)
                        .message("Lấy danh sách môn học của Khoa thành công")
                        .result(scheduleService.getPendingSchedulesForHOD(username, semesterId))
                        .build();
        }

        @GetMapping("/lecturer/{lecturerCode}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'USER','DEAN')")
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

        @GetMapping("/class-section/{classSectionId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'LECTURER', 'USER','DEAN')")
        public ApiResponse<java.util.Map<String, List<ScheduleResponse>>> getSchedulesByClassSection(
                @PathVariable UUID classSectionId) {
                return ApiResponse.<java.util.Map<String, List<ScheduleResponse>>>builder()
                        .code(1000)
                        .message("Lấy thời khóa biểu lớp học phần thành công")
                        .result(scheduleService.getSchedulesByClassSection(classSectionId))
                        .build();
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'LECTURER', 'USER','DEAN')")
        public ApiResponse<ScheduleResponse> getScheduleById(@PathVariable UUID id) {
                return ApiResponse.<ScheduleResponse>builder()
                        .code(1000)
                        .message("Lấy chi tiết lịch học thành công")
                        .result(scheduleService.getScheduleById(id))
                        .build();
        }
}