package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.ScheduleLecturerRequest;
import com.example.datn.DTO.Request.ScheduleRoomRequest;
import com.example.datn.DTO.Response.*;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.*;
import com.example.datn.Pattern.Stragery.scheduling.GreedySchedulerEngine;
import com.example.datn.Pattern.Stragery.scheduling.LecturerSuggestionEngine; // Thêm import này
import com.example.datn.Pattern.Stragery.scheduling.SuggestionEngine;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    // ── CÁC ENGINE CỐT LÕI CỦA HỆ THỐNG ──────────────────────────────────────
    private final GreedySchedulerEngine greedySchedulerEngine;       // Phase 2: Tự xếp lịch
    // SỬA LẠI KIỂU DỮ LIỆU Ở DÒNG DƯỚI NÀY:
    private final LecturerSuggestionEngine lecturerSuggestionEngine; // Phase 3: Gợi ý Giảng viên
    private final SuggestionEngine suggestionEngine;                 // Phase 4: Gợi ý Giờ/Phòng


    @Override
    @Transactional
    public ScheduleInitResponse createSchedule() {
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


    // ── AUTO SCHEDULING & SUGGESTION (PHASE 2, 3, 4) ─────────────────────────

    @Override
    @Transactional
    public AutoAssignResultResponse autoAssignRoomAndTime(UUID semesterId) {
        log.info("[ScheduleService] Nhận request chạy Auto-Scheduling cho học kỳ: {}", semesterId);

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        return greedySchedulerEngine.run(semester.getId());
    }

    @Override
    public List<LecturerSuggestionResponse> suggestLecturersForSchedule(UUID scheduleId) {
        log.info("[ScheduleService] Nhận request gợi ý giảng viên cho Schedule: {}", scheduleId);

        Schedule schedule = findScheduleById(scheduleId);

        if (schedule.getDayOfWeek() == null || schedule.getStartPeriod() == null) {
            throw new AppException(ErrorCode.SCHEDULE_TIME_NOT_SET, "Cần xếp giờ học trước khi tìm giảng viên phù hợp.");
        }

        return lecturerSuggestionEngine.suggest(schedule);
    }

    @Override
    public List<SlotSuggestionResponse> suggestSlotsForSchedule(UUID scheduleId, int topN) {
        log.info("[ScheduleService] Nhận request gợi ý {} slot cho Schedule: {}", topN, scheduleId);
        return suggestionEngine.suggest(scheduleId, topN);
    }


    // ── MANUAL ASSIGNMENT (GIAI ĐOẠN 1 & XỬ LÝ LỖI) ──────────────────────────

    @Override
    @Transactional
    public ScheduleResponse assignTime(UUID scheduleId, com.example.datn.DTO.Request.ScheduleTimeRequest request) {
        Schedule schedule = findScheduleById(scheduleId);
        checkLock(schedule);

        int periods = 3;
        if (schedule.getClassSection() != null && schedule.getClassSection().getSubjectComponent() != null) {
            Integer p = schedule.getClassSection().getSubjectComponent().getPeriodsPerSession();
            if (p != null && p > 0) periods = p;
        }

        Integer startPeriod = request.getStartPeriod();
        Integer endPeriod = startPeriod + periods - 1;

        if (startPeriod > endPeriod || endPeriod > 15) {
            throw new AppException(ErrorCode.INVALID_PERIOD_RANGE);
        }

        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartPeriod(startPeriod);
        schedule.setEndPeriod(endPeriod);

        if (schedule.getRoom() != null) {
            validateRoomConflict(schedule.getRoom(), request.getDayOfWeek(), startPeriod, endPeriod, scheduleId);
        }
        if (schedule.getLecturer() != null) {
            validateLecturerConflict(schedule.getLecturer(), request.getDayOfWeek(), startPeriod, endPeriod, scheduleId);
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

            if (room.getCapacity() != null) {
                int maxStudents = schedule.getClassSection().getCapacity();
                if (maxStudents > room.getCapacity()) {
                    throw new AppException(ErrorCode.ROOM_CAPACITY_EXCEEDED,
                            "Phòng " + room.getName() + " không đủ sức chứa cho lớp có chỉ tiêu " + maxStudents + " SV");
                }
            }

            if (schedule.getDayOfWeek() != null && schedule.getStartPeriod() != null && schedule.getEndPeriod() != null) {
                validateRoomConflict(room, schedule.getDayOfWeek(), schedule.getStartPeriod(), schedule.getEndPeriod(), scheduleId);
            }
        }

        schedule.setRoom(room);
        Schedule saved = scheduleRepository.save(schedule);
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
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ScheduleResponse assignLecturer(UUID scheduleId, ScheduleLecturerRequest request) {
        Schedule schedule = findScheduleById(scheduleId);
        checkLock(schedule);

        if (schedule.getDayOfWeek() == null || schedule.getStartPeriod() == null || schedule.getEndPeriod() == null) {
            throw new AppException(ErrorCode.SCHEDULE_TIME_NOT_SET);
        }

        Lecturer lecturer = null;
        if (request.getLecturerCode() != null) {
            lecturer = lecturerRepository.findByLecturerCode(request.getLecturerCode())
                    .orElseThrow(() -> new AppException(ErrorCode.LECTURER_NOT_FOUND));
            validateLecturerConflict(lecturer, schedule.getDayOfWeek(), schedule.getStartPeriod(), schedule.getEndPeriod(), scheduleId);
        }

        schedule.setLecturer(lecturer);
        Schedule saved = scheduleRepository.save(schedule);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public java.util.List<ScheduleResponse> bulkAssignLecturer(com.example.datn.DTO.Request.BulkScheduleLecturerRequest request) {
        if (request.getScheduleIds() == null || request.getScheduleIds().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách lịch học bị trống");
        }

        List<ScheduleResponse> responses = new java.util.ArrayList<>();
        for (UUID scheduleId : request.getScheduleIds()) {
            ScheduleLecturerRequest singleReq = new ScheduleLecturerRequest(request.getLecturerCode());
            ScheduleResponse singleResponse = assignLecturer(scheduleId, singleReq);
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
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSchedule(UUID id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        checkLock(schedule);
        schedule.setIsDeleted(true);
        scheduleRepository.save(schedule);
    }


    // ── CÁC HÀM GET DỮ LIỆU (READ-ONLY) ──────────────────────────────────────

    @Override
    public org.springframework.data.domain.Page<ScheduleResponse> getSchedulesBySemester(UUID semesterId, org.springframework.data.domain.Pageable pageable) {
        return scheduleRepository.findBySemesterId(semesterId, pageable).map(this::toResponse);
    }

    @Override
    public org.springframework.data.domain.Page<ScheduleResponse> getSchedulesByLecturer(String lecturerCode, UUID semesterId, org.springframework.data.domain.Pageable pageable) {
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
        return scheduleRepository.findByLecturerAndSemester(lecturerCode, currentSemester.getId(), pageable).map(this::toResponse);
    }

    @Override
    public org.springframework.data.domain.Page<ScheduleResponse> getPendingSchedulesForHOD(String username, UUID semesterId, org.springframework.data.domain.Pageable pageable) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Lecturer hod = lecturerRepository.findByUser(user).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Người dùng không phải là giảng viên"));

        return scheduleRepository.findPendingSchedulesByDepartmentAndSemester(hod.getMajor().getName(), semesterId, pageable)
                .map(this::toResponse);
    }

    @Override
    public java.util.Map<String, List<ScheduleResponse>> getSchedulesByClassSection(UUID classSectionId) {
        ClassSection cs = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND));

        List<UUID> idsToFetch = new java.util.ArrayList<>();
        idsToFetch.add(cs.getId());

        // Tìm thêm các anh em họ hàng (child hoặc parent chung môn học)
        List<ClassSection> siblings = classSectionRepository.findBySubjectIdAndSemesterId(
                cs.getSubject().getId(), cs.getSemester().getId());

        UUID parentId = cs.getParentSection() != null ? cs.getParentSection().getId() : cs.getId();
        
        for (ClassSection sibling : siblings) {
            if (!sibling.getId().equals(cs.getId())) {
                boolean isSameFamily = sibling.getId().equals(parentId) 
                        || (sibling.getParentSection() != null && sibling.getParentSection().getId().equals(parentId));
                if (isSameFamily) {
                    idsToFetch.add(sibling.getId());
                }
            }
        }

        List<ScheduleResponse> responses = scheduleRepository.findByClassSection_IdIn(idsToFetch)
                .stream().map(this::toResponse).collect(Collectors.toList());

        // Sort: Subject -> SectionCode -> Day -> StartPeriod
        responses.sort(java.util.Comparator
                .comparing(ScheduleResponse::getSubjectName, java.util.Comparator.nullsLast(String::compareTo))
                .thenComparing(ScheduleResponse::getSectionCode, java.util.Comparator.nullsLast(String::compareTo))
                .thenComparing(ScheduleResponse::getDayOfWeek, java.util.Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ScheduleResponse::getStartPeriod, java.util.Comparator.nullsLast(Integer::compareTo)));

        return responses.stream().collect(Collectors.groupingBy(ScheduleResponse::getSubjectName));
    }

    @Override
    public void exportSemesterScheduleToPdf(UUID semesterId, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        // Bỏ qua unpaged để fetch toàn bộ mảng List lịch
        List<Schedule> allSchedules = scheduleRepository.findBySemesterId(semesterId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        List<Schedule> assignedSchedules = allSchedules.stream()
                .filter(s -> s.getDayOfWeek() != null)
                .collect(Collectors.toList());

        java.util.Map<String, List<ScheduleResponse>> groupedSchedules = assignedSchedules.stream()
                .map(this::toResponse)
                .sorted(java.util.Comparator
                        .comparing(ScheduleResponse::getSubjectName, java.util.Comparator.nullsLast(String::compareTo))
                        .thenComparing(ScheduleResponse::getSectionCode, java.util.Comparator.nullsLast(String::compareTo))
                        .thenComparing(ScheduleResponse::getDayOfWeek, java.util.Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ScheduleResponse::getStartPeriod, java.util.Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.groupingBy(
                        ScheduleResponse::getSubjectName,
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));

        response.setContentType("application/pdf");
        
        String safeSemesterName = java.text.Normalizer.normalize(semester.getName(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", "_");
        response.setHeader("Content-Disposition", "attachment; filename=lich_hoc_ky_" + safeSemesterName + ".pdf");

        try (com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate())) {
            com.lowagie.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12);
            com.lowagie.text.Font normalFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 12);
            com.lowagie.text.Font subjectFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14);

            try {
                com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont("C:\\Windows\\Fonts\\arial.ttf", com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED);
                titleFont = new com.lowagie.text.Font(bf, 18, com.lowagie.text.Font.BOLD);
                headerFont = new com.lowagie.text.Font(bf, 12, com.lowagie.text.Font.BOLD);
                normalFont = new com.lowagie.text.Font(bf, 12, com.lowagie.text.Font.NORMAL);
                subjectFont = new com.lowagie.text.Font(bf, 14, com.lowagie.text.Font.BOLD);
            } catch (Exception e) {
                log.warn("[PDF] Could not load Arial font, falling back to Helvetica");
            }

            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("LỊCH HỌC KỲ DỰ KIẾN - " + semester.getName(), titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(title);
            document.add(new com.lowagie.text.Paragraph("\n"));

            if (groupedSchedules.isEmpty()) {
                document.add(new com.lowagie.text.Paragraph("Chưa có lịch học nào được xếp thời gian.", normalFont));
                return;
            }

            for (java.util.Map.Entry<String, List<ScheduleResponse>> entry : groupedSchedules.entrySet()) {
                document.add(new com.lowagie.text.Paragraph("Môn học: " + entry.getKey(), subjectFont));
                document.add(new com.lowagie.text.Paragraph("\n"));

                com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.5f, 1f, 1f, 1.5f, 2f, 3f});

                String[] headers = {"Mã Lớp", "Thứ", "Tiết", "Thời lượng", "Phòng", "Giảng viên"};
                for (String header : headers) {
                    com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(header, headerFont));
                    cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                for (ScheduleResponse s : entry.getValue()) {
                    table.addCell(new com.lowagie.text.Phrase(s.getSectionCode() != null ? s.getSectionCode() : "", normalFont));
                    table.addCell(new com.lowagie.text.Phrase(s.getDayOfWeekName() != null ? s.getDayOfWeekName() : "", normalFont));
                    table.addCell(new com.lowagie.text.Phrase(s.getStartPeriod() != null ? s.getStartPeriod().toString() : "", normalFont));
                    
                    String periods = (s.getStartPeriod() != null && s.getEndPeriod() != null) 
                            ? (s.getEndPeriod() - s.getStartPeriod() + 1) + " tiết" : "";
                    table.addCell(new com.lowagie.text.Phrase(periods, normalFont));
                    
                    table.addCell(new com.lowagie.text.Phrase(s.getRoomName() != null ? s.getRoomName() : "E-Learning", normalFont));
                    table.addCell(new com.lowagie.text.Phrase(s.getLecturerName() != null ? s.getLecturerName() : "Chưa phân công", normalFont));
                }

                document.add(table);
                document.add(new com.lowagie.text.Paragraph("\n"));
            }
        }
    }

    @Override
    public ScheduleResponse getScheduleById(UUID id) {
        return toResponse(findScheduleById(id));
    }


    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private Schedule findScheduleById(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void checkLock(Schedule schedule) {
        if (Boolean.TRUE.equals(schedule.getIsLocked())) {
            throw new AppException(ErrorCode.SCHEDULE_IS_LOCKED);
        }
    }

    private void validateRoomConflict(Room room, Integer dayOfWeek, Integer startPeriod, Integer endPeriod, UUID excludeId) {
        if (dayOfWeek == null || startPeriod == null || endPeriod == null) {
            throw new AppException(ErrorCode.INVALID_SCHEDULE_TIME);
        }
        List<Schedule> conflicts = scheduleRepository.findConflictingByRoom(room.getId(), dayOfWeek, startPeriod, endPeriod, excludeId);
        if (!conflicts.isEmpty()) {
            throw new AppException(ErrorCode.ROOM_CONFLICT);
        }
    }

    private void validateLecturerConflict(Lecturer lecturer, Integer dayOfWeek, Integer startPeriod, Integer endPeriod, UUID excludeId) {
        if (dayOfWeek == null || startPeriod == null || endPeriod == null) {
            throw new AppException(ErrorCode.INVALID_SCHEDULE_TIME);
        }
        List<Schedule> conflicts = scheduleRepository.findConflictingByLecturer(lecturer.getId(), dayOfWeek, startPeriod, endPeriod, excludeId);
        if (!conflicts.isEmpty()) {
            throw new AppException(ErrorCode.LECTURER_CONFLICT);
        }
    }

    private ScheduleResponse toResponse(Schedule s) {
        return ScheduleResponse.builder()
                .id(s.getId())
                .classSectionId(s.getClassSection().getId())
                .sectionCode(s.getClassSection().getSectionCode())
                .subjectName(s.getClassSection().getSubject().getName())
                .subjectCode(s.getClassSection().getSubject().getCode())
                .roomId(s.getRoom() != null ? s.getRoom().getId() : null)
                .roomName(s.getRoom() != null ? s.getRoom().getName() : null)
                .lecturerId(s.getLecturer() != null ? s.getLecturer().getId() : null)
                .lecturerName(s.getLecturer() != null ? s.getLecturer().getFullName() : null)
                .lecturerCode(s.getLecturer() != null ? s.getLecturer().getLecturerCode() : null)
                .dayOfWeek(s.getDayOfWeek())
                .dayOfWeekName(toDayName(s.getDayOfWeek()))
                .startPeriod(s.getStartPeriod())
                .endPeriod(s.getEndPeriod())
                .build();
    }

    private String toDayName(Integer dayOfWeek) {
        if (dayOfWeek == null) return null;
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