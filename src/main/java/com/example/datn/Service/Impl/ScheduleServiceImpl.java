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
    private final MajorRepository majorRepository;

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
        checkLock(schedule);

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
    public ScheduleResponse clearTime(UUID scheduleId) {
        Schedule schedule = findScheduleById(scheduleId);
        checkLock(schedule);
        if (schedule.getDayOfWeek() == null && schedule.getStartPeriod() == null && schedule.getEndPeriod() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lịch học chưa được xếp thời gian, không thể hủy bỏ");
        }
        
        schedule.setDayOfWeek(null);
        schedule.setStartPeriod(null);
        schedule.setEndPeriod(null);
        
        Schedule saved = scheduleRepository.save(schedule);
        log.info("[Schedule] Đã bỏ phân công thời gian cho schedule {}", scheduleId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ScheduleResponse assignRoom(UUID scheduleId, ScheduleRoomRequest request) {

        Schedule schedule = findScheduleById(scheduleId);
        checkLock(schedule);

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
    public ScheduleResponse clearRoom(UUID scheduleId) {
        Schedule schedule = findScheduleById(scheduleId);
        checkLock(schedule);
        if (schedule.getRoom() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lịch học chưa được xếp phòng, không thể hủy bỏ");
        }

        schedule.setRoom(null);
        
        Schedule saved = scheduleRepository.save(schedule);
        log.info("[Schedule] Đã bỏ phân công phòng cho schedule {}", scheduleId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ScheduleResponse assignLecturer(UUID scheduleId, ScheduleLecturerRequest request) {
        log.info("[Schedule] HOD phân công giảng viên {} cho schedule {}", request.getLecturerCode(), scheduleId);

        Schedule schedule = findScheduleById(scheduleId);
        checkLock(schedule);

        // Bít kẽ hở: bắt buộc phải có đầy đủ giờ học trước khi phân công
        if (schedule.getDayOfWeek() == null || schedule.getStartPeriod() == null
                || schedule.getEndPeriod() == null) {
            log.warn("[Schedule] Schedule {} chưa có giờ học, không thể phân công giảng viên", scheduleId);
            throw new AppException(ErrorCode.SCHEDULE_TIME_NOT_SET);
        }

        Lecturer lecturer = null;
        if (request.getLecturerCode() != null) {
            lecturer = lecturerRepository.findByLecturerCode(request.getLecturerCode())
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

    @Override
    @Transactional
    public java.util.List<ScheduleResponse> bulkAssignLecturer(com.example.datn.DTO.Request.BulkScheduleLecturerRequest request) {
        log.info("[Schedule] HOD phân công giảng viên {} cho hàng loạt schedule: {}", request.getLecturerCode(), request.getScheduleIds());
        
        if (request.getScheduleIds() == null || request.getScheduleIds().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách lịch học bị trống");
        }

        List<ScheduleResponse> responses = new java.util.ArrayList<>();
        for (UUID scheduleId : request.getScheduleIds()) {
            // Re-use logic từ hàm assignLecturer gốc
            // Tuy nhiên vì cần DB update ngay để validate conflict chéo, ta sẽ ép flush
            ScheduleLecturerRequest singleReq = new ScheduleLecturerRequest(request.getLecturerCode());
            ScheduleResponse singleResponse = assignLecturer(scheduleId, singleReq);
            // Ép Hibernate xả dữ liệu xuống DB ngay sau mỗi vòng lặp để các câu query validateLecturerConflict tiếp theo nhìn thấy dữ liệu mới thay vì bị ẩn trong cached Session.
            scheduleRepository.flush(); 
            responses.add(singleResponse);
        }

        return responses;
    }

    @Override
    @Transactional
    public ScheduleResponse clearLecturer(UUID scheduleId) {
        Schedule schedule = findScheduleById(scheduleId);
        checkLock(schedule);
        if (schedule.getLecturer() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lịch học chưa được phân công giảng viên, không thể hủy bỏ");
        }

        schedule.setLecturer(null);
        
        Schedule saved = scheduleRepository.save(schedule);
        log.info("[Schedule] Đã bỏ phân công giảng viên cho schedule {}", scheduleId);
        return toResponse(saved);
    }

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
        checkLock(schedule);
        schedule.setIsDeleted(true);
        scheduleRepository.save(schedule);
        log.info("[Schedule] Đã soft-delete schedule {}", id);
    }

    // ── ADMIN: Xem lịch toàn học kỳ ──────────────────────────────────────────

    @Override
    public org.springframework.data.domain.Page<ScheduleResponse> getSchedulesBySemester(UUID semesterId, org.springframework.data.domain.Pageable pageable) {
        return scheduleRepository.findBySemesterId(semesterId, pageable)
                .map(this::toResponse);
    }

    // ── GIẢNG VIÊN: Xem lịch dạy ─────────────────────────────────────────────

    @Override
    public org.springframework.data.domain.Page<ScheduleResponse> getSchedulesByLecturer(String lecturerCode, UUID semesterId, org.springframework.data.domain.Pageable pageable) {
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));

        return scheduleRepository.findByLecturerAndSemester(lecturerCode, currentSemester.getId(), pageable)
                .map(this::toResponse);
    }



    @Override
    public org.springframework.data.domain.Page<ScheduleResponse> getPendingSchedulesForHOD(String username, UUID semesterId, org.springframework.data.domain.Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Lecturer hod = lecturerRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Người dùng không phải là giảng viên"));

        String hodDepartmentName = hod.getMajor().getName();

        org.springframework.data.domain.Page<Schedule> schedules = scheduleRepository.findPendingSchedulesByDepartmentAndSemester(hodDepartmentName,
                semesterId, pageable);

        return schedules.map(this::toResponse);
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

//    public List<ScheduleResponse> autoSetTime(){
//        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
//                .orElseThrow(() -> new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
//       // Major peMajor = majorRepository.findByCode("PE").orElseThrow(()-> new AppException(ErrorCode.MAJOR_NOT_FOUND));
//        // List<Schedule> peSchedules = scheduleRepository.findBySemesterIdAndDepartmentName(currentSemester.getId(), peMajor.getName());
//        Major elearningMajor = majorRepository.findByCode("NLCT").orElseThrow(()-> new AppException(ErrorCode.MAJOR_NOT_FOUND));
//        List<Schedule> elearningSchedules = scheduleRepository.findBySemesterIdAndDepartmentName(currentSemester.getId(), elearningMajor.getName());
//        elearningSchedules.stream().map(
//                s -> {
//                    s.setDayOfWeek(8);
//                    s.setStartPeriod(1);
//                    s.setEndPeriod(3);
//                    return s;
//                })
//                        .collect(Collectors.toList());
//        scheduleRepository.saveAll(elearningSchedules);
//        List<Schedule> schedules=scheduleRepository.find
//
//
//    }

    private Schedule findScheduleById(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void checkLock(Schedule schedule) {
        if (Boolean.TRUE.equals(schedule.getIsLocked())) {
            log.warn("[Schedule] Từ chối thao tác - lịch học {} đã bị khóa", schedule.getId());
            throw new AppException(ErrorCode.SCHEDULE_IS_LOCKED);
        }
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
