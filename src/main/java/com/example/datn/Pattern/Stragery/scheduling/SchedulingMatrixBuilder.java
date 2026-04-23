package com.example.datn.Pattern.Stragery.scheduling;

import com.example.datn.Model.Lecturer;
import com.example.datn.Model.Room;
import com.example.datn.Model.Schedule;
import com.example.datn.Repository.LecturerRepository;
import com.example.datn.Repository.RoomRepository;
import com.example.datn.Repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Phase 1 – Đọc DB và build toàn bộ in-memory matrices vào {@link SchedulingContext}.
 *
 * <p>Chỉ đọc DB 1 lần duy nhất. Phase 2 (Greedy) sẽ chỉ đọc RAM từ Context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulingMatrixBuilder {

    private final ScheduleRepository scheduleRepository;
    private final LecturerRepository lecturerRepository;
    private final RoomRepository     roomRepository;
    private final com.example.datn.Repository.ClassSectionRepository classSectionRepository;

    /**
     * Khởi tạo {@link SchedulingContext} đầy đủ cho một semester.
     *
     * <ol>
     *   <li>Load schedules đã có room+time → build roomBusy</li>
     *   <li>Load schedules đã có lecturer → build lecturerBusy + lecturerCurrentLoad</li>
     *   <li>Load schedules đã có subject concurrent → build subjectConcurrentMatrix</li>
     *   <li>Tính maxConcurrentBySubject = số GV có major khớp subject</li>
     * </ol>
     */
    public SchedulingContext build(UUID semesterId) {
        log.info("[MatrixBuilder] Bắt đầu build scheduling context cho semester {}", semesterId);
        SchedulingContext ctx = new SchedulingContext(semesterId);

        // Load tất cả schedule đã có thông tin room+time (dùng để build roomBusy)
        List<Schedule> assignedSchedules = scheduleRepository.findAssignedSchedules(semesterId);
        log.info("[MatrixBuilder] Tìm thấy {} schedule đã có phòng+giờ", assignedSchedules.size());

        for (Schedule s : assignedSchedules) {
            if (s.getRoom() == null || s.getDayOfWeek() == null
                    || s.getStartPeriod() == null || s.getEndPeriod() == null) {
                continue;
            }

            UUID roomId     = s.getRoom().getId();
            int  day        = s.getDayOfWeek();
            int  start      = s.getStartPeriod();
            int  end        = s.getEndPeriod();
            UUID subjectId  = s.getClassSection().getSubject().getId();

            // 1. Room busy matrix
            ctx.setRoomBusy(roomId, day, start, end, true);

            // 2. Subject concurrent matrix (đếm lớp mở song song)
            ctx.updateSubjectConcurrent(subjectId, day, start, end, +1);

            // 3. Lecturer busy matrix + load
            if (s.getLecturer() != null) {
                UUID lecturerId = s.getLecturer().getId();
                ctx.setLecturerBusy(lecturerId, day, start, end, true);
                ctx.addLecturerLoad(lecturerId, end - start + 1);
            }
        }

        // 4. maxConcurrentBySubject: đếm số GV theo faculty (hoặc major) có thể dạy môn đó
        //    Cách đơn giản nhất: đếm số GV có departmentName trùng với subject.departmentName
        //    Nếu sau này có bảng "lecturer_subjects" thì thay thế bằng join đó
        List<Lecturer> allLecturers = lecturerRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();

        // Đếm GV theo tên khoa để tính maxConcurrent mặc định
        // (1 GV dạy được tối đa 1 lớp song song → maxConcurrent = số GV cùng khoa)
        java.util.Map<String, Long> lecturerCountByDept = allLecturers.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        l -> l.getMajor() != null ? l.getMajor().getName() : "UNKNOWN",
                        java.util.stream.Collectors.counting()
                ));

        // Đối với mỗi subject đã xuất hiện trong context, tính maxConcurrent
        // Load distinct subjects trong semester (kể cả chưa có lịch)
        classSectionRepository.findDistinctSubjectsBySemesterId(semesterId)
                .forEach(subject -> {
                    long count = lecturerCountByDept.getOrDefault(subject.getDepartmentName(), 1L);
                    ctx.getMaxConcurrentBySubject().put(subject.getId(), (int) count);
                });

        log.info("[MatrixBuilder] Context built xong — {} phòng bận, {} GV bận",
                ctx.getRoomBusy().size(), ctx.getLecturerBusy().size());
        return ctx;
    }
}
