package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.ScheduleLecturerRequest;
import com.example.datn.DTO.Request.ScheduleRoomRequest;
import com.example.datn.DTO.Response.ScheduleInitResponse;
import com.example.datn.DTO.Response.ScheduleResponse;
import com.example.datn.DTO.Response.SubjectResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IClassSectionService;
import com.example.datn.Service.Interface.IScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements IScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ClassSectionRepository classSectionRepository;

    private final LecturerRepository lecturerRepository;
    private final RoomRepository roomRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional

    public ScheduleInitResponse createSchedule() { // Đổi kiểu trả về ở đây
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
        long totalSections = classSectionRepository.countBySemesterId(currentSemester.getId());
        if (totalSections == 0) {
            throw new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND);
        }

        List<ClassSection> newSections = classSectionRepository.findSectionsWithoutSchedule(currentSemester.getId());

        long alreadyExists = scheduleRepository.countByClassSection_Semester_Id(currentSemester.getId());

        if (!newSections.isEmpty()) {
            List<Schedule> schedules = newSections.stream()
                    .map(section -> Schedule.builder().classSection(section).build())
                    .collect(Collectors.toList());
            scheduleRepository.saveAll(schedules);
            log.info("[Schedule] Đã tạo {} lịch mới cho học kỳ {}", newSections.size(), currentSemester.getName());
        } else {
            log.info("[Schedule] Tất cả {} lớp đã có lịch — không tạo thêm", totalSections);
        }

        return ScheduleInitResponse.builder()
                .semesterId(currentSemester.getId())
                .semesterName(currentSemester.getName())
                .totalSections(totalSections)
                .alreadyHadSchedule(alreadyExists)
                .newlyCreated((long) newSections.size())
                .message(newSections.isEmpty()
                        ? "Tất cả lớp học phần đã có lịch"
                        : "Đã tạo thành công " + newSections.size() + " lịch mới")
                .build();
    }


    @Override
    @Transactional
    public ScheduleResponse assignTime(UUID scheduleId, com.example.datn.DTO.Request.ScheduleTimeRequest request) {

        Schedule schedule = findScheduleById(scheduleId);

        int periods = 3; // Default
        if (schedule.getClassSection() != null && schedule.getClassSection().getSubjectComponent() != null) {
            Integer p = schedule.getClassSection().getSubjectComponent().getPeriodsPerSession();
            if (p != null && p > 0) periods = p;
        }

        Integer startPeriod = request.getStartPeriod();
        
        // Tự động tính toán endPeriod dựa theo thời lượng cấu hình của lớp học
        Integer endPeriod = startPeriod + periods - 1;
        log.info("[Schedule] Auto-calculated endPeriod: {} (start: {}, periods: {})", endPeriod, startPeriod, periods);

        if (startPeriod > endPeriod || endPeriod > 15) {
            throw new AppException(ErrorCode.INVALID_PERIOD_RANGE);
        }

        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartPeriod(startPeriod);
        schedule.setEndPeriod(endPeriod);

        // Nếu đã có phòng rồi, thì khi đổi thời gian, cần check lại xem phòng có bị
        // trùng không
        if (schedule.getRoom() != null) {
            validateRoomConflict(schedule.getRoom(), request.getDayOfWeek(),
                    startPeriod, endPeriod, scheduleId);
        }

        // Tương tự, nếu đã có giảng viên, đổi giờ có thể gây trùng lịch giảng viên
        if (schedule.getLecturer() != null) {
            validateLecturerConflict(schedule.getLecturer(), request.getDayOfWeek(),
                    startPeriod, endPeriod, scheduleId);
        }

        Schedule saved = scheduleRepository.save(schedule);
        log.info("[Schedule] Đã xếp thời gian (Thứ {}, tiết {}-{}) cho schedule {}",
                request.getDayOfWeek(), startPeriod, endPeriod, scheduleId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ScheduleResponse assignRoom(UUID scheduleId, ScheduleRoomRequest request) {

        Schedule schedule = findScheduleById(scheduleId);

        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findByIdForUpdate(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

            // Kiểm tra sức chứa
            if (room.getCapacity() != null) {
                int maxStudents = schedule.getClassSection().getCapacity();
                if (maxStudents > room.getCapacity()) {
                    log.warn("[Schedule] Phòng {} ({} chỗ) không đủ cho lớp {} SV",
                            room.getName(), room.getCapacity(), maxStudents);
                    throw new AppException(ErrorCode.ROOM_CAPACITY_EXCEEDED,
                            "Phòng " + room.getName() + " (Chứa " + room.getCapacity() + " chỗ) "
                                    + "không đủ sức chứa cho lớp có chỉ tiêu " + maxStudents + " SV");
                }
            }

            // LAYER 2: Chỉ check conflict nếu thời gian ĐÃ ĐƯỢC XẾP từ trước (bởi
            // assignTime)
            if (schedule.getDayOfWeek() != null && schedule.getStartPeriod() != null
                    && schedule.getEndPeriod() != null) {
                validateRoomConflict(room, schedule.getDayOfWeek(),
                        schedule.getStartPeriod(), schedule.getEndPeriod(), scheduleId);
            }
        }

        schedule.setRoom(room);

        Schedule saved = scheduleRepository.save(schedule);
        log.info("[Schedule] Đã xếp phòng {} cho schedule {}",
                room != null ? room.getName() : "null", scheduleId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ScheduleResponse assignLecturer(UUID scheduleId, ScheduleLecturerRequest request) {
        log.info("[Schedule] HOD phân công giảng viên {} cho schedule {}", request.getLecturerId(), scheduleId);

        Schedule schedule = findScheduleById(scheduleId);

        // Bít kẽ hở: bắt buộc phải có đầy đủ giờ học trước khi phân công
        if (schedule.getDayOfWeek() == null || schedule.getStartPeriod() == null
                || schedule.getEndPeriod() == null) {
            log.warn("[Schedule] Schedule {} chưa có giờ học, không thể phân công giảng viên", scheduleId);
            throw new AppException(ErrorCode.SCHEDULE_TIME_NOT_SET);
        }

        Lecturer lecturer = null;
        if (request.getLecturerId() != null) {
            lecturer = lecturerRepository.findById(request.getLecturerId())
                    .orElseThrow(() -> new AppException(ErrorCode.LECTURER_NOT_FOUND));
            validateLecturerConflict(lecturer, schedule.getDayOfWeek(),
                    schedule.getStartPeriod(), schedule.getEndPeriod(), scheduleId);
        }

        schedule.setLecturer(lecturer);
        Schedule saved = scheduleRepository.save(schedule);
        log.info("[Schedule] Đã phân công giảng viên {} cho schedule {}",
                lecturer != null ? lecturer.getLecturerCode() : "null (hủy phân công)",
                scheduleId);
        return toResponse(saved);
    }

    // ── ADMIN: Xóa lịch (Soft Delete) ────────────────────────────────────────

    @Override
    @Transactional
    public void deleteSchedule(UUID id) {
        // SOFT DELETE — không xóa vật lý, chỉ đánh dấu isDeleted = true
        // Giữ lại lịch sử, audit trail, và tránh mất dữ liệu tham chiếu
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[Schedule] Không tìm thấy schedule {} để xóa", id);
                    return new AppException(ErrorCode.SCHEDULE_NOT_FOUND);
                });
        schedule.setIsDeleted(true);
        scheduleRepository.save(schedule);
        log.info("[Schedule] Đã soft-delete schedule {}", id);
    }

    // ── ADMIN: Xem lịch toàn học kỳ ──────────────────────────────────────────

    @Override
    public List<ScheduleResponse> getSchedulesBySemester(UUID semesterId) {
        return scheduleRepository.findBySemesterId(semesterId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── GIẢNG VIÊN: Xem lịch dạy ─────────────────────────────────────────────

    @Override
    public List<ScheduleResponse> getSchedulesByLecturer(UUID lecturerId, UUID semesterId) {
        return scheduleRepository.findByLecturerAndSemester(lecturerId, semesterId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── HOD: Xem lịch chờ gán giảng viên theo khoa ───────────────────────────

    @Override
    public List<ScheduleResponse> getPendingSchedulesForHOD(String username, UUID semesterId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Lecturer hod = lecturerRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Người dùng không phải là giảng viên"));

        String hodDepartmentName = hod.getFaculty().getName();

        List<Schedule> schedules = scheduleRepository.findPendingSchedulesByDepartmentAndSemester(hodDepartmentName,
                semesterId);

        return schedules.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleResponse> getSchedulesByClassSection(UUID classSectionId) {
        return scheduleRepository.findByClassSection_Id(classSectionId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }


    @Override
    public ScheduleResponse getScheduleById(UUID id) {
        return toResponse(findScheduleById(id));
    }

    private Schedule findScheduleById(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void validateRoomConflict(Room room, Integer dayOfWeek,
            Integer startPeriod, Integer endPeriod,
            UUID excludeId) {
        if (dayOfWeek == null || startPeriod == null || endPeriod == null) {
            throw new AppException(ErrorCode.INVALID_SCHEDULE_TIME);
        }
        List<Schedule> conflicts = scheduleRepository.findConflictingByRoom(
                room.getId(), dayOfWeek, startPeriod, endPeriod, excludeId);
        if (!conflicts.isEmpty()) {
            log.warn("[Schedule] Phát hiện conflict phòng {} vào Thứ {} tiết {}-{}",
                    room.getName(), dayOfWeek, startPeriod, endPeriod);
            throw new AppException(ErrorCode.ROOM_CONFLICT);
        }
    }

    private void validateLecturerConflict(Lecturer lecturer, Integer dayOfWeek,
            Integer startPeriod, Integer endPeriod,
            UUID excludeId) {
        if (dayOfWeek == null || startPeriod == null || endPeriod == null) {
            throw new AppException(ErrorCode.INVALID_SCHEDULE_TIME);
        }
        List<Schedule> conflicts = scheduleRepository.findConflictingByLecturer(
                lecturer.getId(), dayOfWeek, startPeriod, endPeriod, excludeId);
        if (!conflicts.isEmpty()) {
            log.warn("[Schedule] Phát hiện conflict giảng viên {} vào Thứ {} tiết {}-{}",
                    lecturer.getLecturerCode(), dayOfWeek, startPeriod, endPeriod);
            throw new AppException(ErrorCode.LECTURER_CONFLICT);
        }
    }

    private UUID getRoomId(Room r) {
        return r != null ? r.getId() : null;
    }

    private String getRoomName(Room r) {
        return r != null ? r.getName() : null;
    }

    private UUID getLecturerId(Lecturer l) {
        return l != null ? l.getId() : null;
    }

    private String getLecturerName(Lecturer l) {
        return l != null ? l.getFullName() : null;
    }

    private String getLecturerCode(Lecturer l) {
        return l != null ? l.getLecturerCode() : null;
    }

    private ScheduleResponse toResponse(Schedule s) {
        return ScheduleResponse.builder()
                .id(s.getId())
                .classSectionId(s.getClassSection().getId())
                .sectionCode(s.getClassSection().getSectionCode())
                .subjectName(s.getClassSection().getSubject().getName())
                .subjectCode(s.getClassSection().getSubject().getCode())
                .roomId(getRoomId(s.getRoom()))
                .roomName(getRoomName(s.getRoom()))
                .lecturerId(getLecturerId(s.getLecturer()))
                .lecturerName(getLecturerName(s.getLecturer()))
                .lecturerCode(getLecturerCode(s.getLecturer()))
                .dayOfWeek(s.getDayOfWeek())
                .dayOfWeekName(toDayName(s.getDayOfWeek()))
                .startPeriod(s.getStartPeriod())
                .endPeriod(s.getEndPeriod())
                .build();
    }

    private String toDayName(Integer dayOfWeek) {
        if (dayOfWeek == null)
            return null;
        return switch (dayOfWeek) {
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            case 8 -> "Chủ nhật";
            default -> "Không xác định";
        };
    }
}
