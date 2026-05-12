    package com.example.datn.Pattern.Stragery.scheduling;

    import com.example.datn.DTO.Response.AutoAssignResultResponse;
    import com.example.datn.Exception.AppException;
    import com.example.datn.Exception.ErrorCode;
    import com.example.datn.Model.*;
    import com.example.datn.Repository.ClassSectionRepository;
    import com.example.datn.Repository.RoomRepository;
    import com.example.datn.Repository.ScheduleRepository;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Component;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.*;
    import java.util.concurrent.ConcurrentHashMap;
    import java.util.stream.Collectors;

    @Slf4j
    @Component
    @RequiredArgsConstructor
    public class GreedySchedulerEngine {

        private static final int[] WORKING_DAYS = {2, 3, 4, 5, 6, 7};
        // Đã nâng lên 16 để cho phép rải môn E-learning vào tiết 14-16
        private static final int MAX_PERIOD = 16;
        private static final UUID NULL_TYPE_KEY = UUID.fromString("00000000-0000-0000-0000-000000000000");
        private static final String ELEARNING_TYPE_ID = "f6a7b8c9-d0e1-2f3a-4b5c-6d7e8f9a0b1c";

        public static final String SUCCESS = "SUCCESS";
        public static final String ERR_NO_ROOM = "NO_ROOM_AVAILABLE";
        public static final String ERR_CAPACITY = "TEACHER_CAPACITY_LIMIT";
        public static final String ERR_SLOT = "NO_VALID_SLOT";
        public static final String ERR_PARENT_CHILD_CONFLICT = "PARENT_CHILD_CONFLICT";

        @Value("${scheduling.rules.allow-cross-lunch:false}")
        private boolean allowCrossLunch;

        private static final Map<Integer, List<Integer>> VALID_STARTS_CACHE = new ConcurrentHashMap<>(8);
        static {
            VALID_STARTS_CACHE.put(2, List.of(1, 7, 3, 9, 5, 11));
            VALID_STARTS_CACHE.put(3, List.of(1, 7, 4, 10));
            VALID_STARTS_CACHE.put(4, List.of(1, 7));
        }

        private final SchedulingMatrixBuilder matrixBuilder;
        private final ScheduleRepository scheduleRepository;
        private final RoomRepository roomRepository;
        private final ClassSectionRepository classSectionRepository;

        private boolean isElearning(Schedule s) {
            SubjectComponent comp = s.getClassSection().getSubjectComponent();
            return comp != null && comp.getRequiredRoomType() != null
                    && comp.getRequiredRoomType().getId().toString().equals(ELEARNING_TYPE_ID);
        }

        @Transactional
        public AutoAssignResultResponse run(UUID semesterId) {
            log.info("[Greedy] === START Production RC Auto-Schedule semester {} ===", semesterId);
            //nạp tất cả các phòng và  sort lại theo sức chứa tăng dần để ưu tiên xếp phòng nhỏ trước
            List<Room> allRooms = roomRepository.findAll();
            allRooms.sort(Comparator.comparingInt(r -> r.getCapacity() == null ? 0 : r.getCapacity()));
            // Nhóm phòng theo loại để khi xếp schedule có yêu cầu loại phòng thì chỉ duyệt trong nhóm đó
            Map<UUID, List<Room>> roomsByType = new HashMap<>();
            for (Room r : allRooms) {
                UUID key = (r.getRoomType() == null) ? NULL_TYPE_KEY : r.getRoomType().getId();
                roomsByType.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            }
            //sắp xếp danh sách phòng trong mỗi loại theo sức chứa tăng dần để ưu tiên xếp phòng nhỏ trước
            roomsByType.values().forEach(list ->
                    list.sort(Comparator.comparingInt(r -> r.getCapacity() == null ? 0 : r.getCapacity()))
            );

            List<Schedule> allSemesterSchedules = scheduleRepository.findBySemesterId(semesterId, org.springframework.data.domain.Pageable.unpaged()).getContent();
            //Lấy   phòng học chưa được xếp lịch
            List<Schedule> originalUnassigned = allSemesterSchedules.stream()
                    .filter(s -> s.getDayOfWeek() == null || s.getStartPeriod() == null)
                    .collect(Collectors.toList());

            //sắp xếp các môn E-learning trước vì không bị ràng buộc về phòng học, chỉ cần xếp vào slot cố định 14-16 và rải đều theo ngày trong tuần để tránh dồn hết vào 1-2 ngày
            int elearningDayIndex = 0;
            for (Schedule s : originalUnassigned) {
                if (isElearning(s)) {
                    s.setRoom(null); // Học online nên không xếp phòng

                    // Rải đều theo thứ tự: 2, 3, 4, 5, 6, 7 rồi lặp lại
                    int assignedDay = WORKING_DAYS[elearningDayIndex % WORKING_DAYS.length];
                    s.setDayOfWeek(assignedDay);
                    s.setStartPeriod(14);
                    s.setEndPeriod(16);

                    elearningDayIndex++;
                }
            }
            //lấy những  lịch còn lại ngoại trừ E
            List<Schedule> schedulingList = originalUnassigned.stream()
                    .filter(s -> !isElearning(s))
                    .collect(Collectors.toList());
            //loại bỏ các lịch đã được cố định nếu có
            Set<UUID> pinnedScheduleIds = schedulingList.stream()
                    .filter(s -> s.getDayOfWeek() != null && s.getStartPeriod() != null)
                    .map(Schedule::getId)
                    .collect(Collectors.toSet());
            //đếm số lượng  phòng đủ điều kiện cho mỗi lịch =>ưu tiên xếp những lịch có ít lựa chọn
            Map<UUID, Integer> eligibleRoomCache = new HashMap<>();
            for (Schedule s : schedulingList) {
                if (!pinnedScheduleIds.contains(s.getId())) {
                    eligibleRoomCache.put(s.getId(), getEligibleRoomCount(s, roomsByType, allRooms));
                }
            }
            //Xếp lịch parent để không xung đột với lớp con
            Map<UUID, List<ClassSection>> childrenCache = new HashMap<>();
            for (Schedule s : allSemesterSchedules) {
                ClassSection section = s.getClassSection();
                if (section.getParentSection() == null) {
                    childrenCache.computeIfAbsent(section.getId(), k -> classSectionRepository.findByParentSectionId(section.getId()));
                }
            }

            int maxAttempts = 5;
            AutoAssignResultResponse bestResult = null;
            Map<UUID, ScheduleStateInfo> bestAllocations = new HashMap<>();

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                log.info("[Greedy] Running Attempt {}/{}", attempt, maxAttempts);

                List<Schedule> unassigned = new ArrayList<>(schedulingList);
                //nếu không xếp được ở lần đầu tiên thì reset lại trạng thái của những lịch chưa được cố định để thử xếp lại ở các lần tiếp theo, tránh trường hợp thứ tự x
                if (attempt > 1) {
                    unassigned.forEach(s -> {
                        if (!pinnedScheduleIds.contains(s.getId())) resetScheduleState(s);
                    });
                }
                //Nếu là lần chạy đầu thì ưu tiên xếp phòng khó trước
                if (attempt == 1) {
                    unassigned.sort(Comparator
                            .comparingInt((Schedule s) -> eligibleRoomCache.getOrDefault(s.getId(), 999))
                            .thenComparing(Comparator.comparingInt(
                                    (Schedule s) -> Optional.ofNullable(s.getClassSection().getCapacity()).orElse(0)
                            ).reversed())
                    );
                } else {
                    //Nếu lần đầu chạy không thành công thì đảo
                    Collections.shuffle(unassigned);
                }
                //build context mới cho mỗi lần thử để đảm bảo thông tin về phòng và giáo viên được cập nhật chính xác theo trạng thái hiện tại của schedule
                SchedulingContext ctx = matrixBuilder.build(semesterId);

                List<Schedule> toSave = new ArrayList<>();
                List<AutoAssignResultResponse.FailedScheduleInfo> failed = new ArrayList<>();
                //Bắt đầu xếp lịch cho những lịch chưa được cố định
                for (Schedule schedule : unassigned) {
                    // Nếu  không có phòng đáp ứng được thì bỏ qua
                    if (!pinnedScheduleIds.contains(schedule.getId()) && eligibleRoomCache.getOrDefault(schedule.getId(), 0) == 0) {
                        failed.add(buildFailedInfo(schedule, ERR_NO_ROOM));
                        continue;
                    }
                    // Thử xếp lịch, kết quả sẽ là SUCCESS hoặc lý do thất bại
                    String result = tryPlace(schedule, roomsByType, allRooms, ctx, pinnedScheduleIds, allSemesterSchedules, childrenCache);

                    if (!SUCCESS.equals(result)) {
                        failed.add(buildFailedInfo(schedule, result));
                    } else {
                        toSave.add(schedule);
                    }
                }

                if (!failed.isEmpty()) {
                    Map<String, Long> failStats = failed.stream()
                            .collect(Collectors.groupingBy(AutoAssignResultResponse.FailedScheduleInfo::getReason, Collectors.counting()));
                    log.warn("[Greedy] Attempt {} Fail Stats: {}", attempt, failStats);
                }

                double rate = unassigned.isEmpty() ? 100 : (toSave.size() * 100.0 / unassigned.size());
                AutoAssignResultResponse currentResult = AutoAssignResultResponse.builder()
                        .placed(toSave.size())
                        .failed(failed.size())
                        .successRate(rate)
                        .failedSchedules(failed)
                        .build();

                if (bestResult == null || currentResult.getSuccessRate() > bestResult.getSuccessRate()) {
                    bestResult = currentResult;
                    bestAllocations.clear();
                    for (Schedule s : toSave) {
                        bestAllocations.put(s.getId(), new ScheduleStateInfo(
                                s.getRoom(), s.getDayOfWeek(), s.getStartPeriod(), s.getEndPeriod()
                        ));
                    }
                }

                if (bestResult.getSuccessRate() >= 100.0) break;
            }

            List<Schedule> finalToSave = new ArrayList<>();
            for (Schedule s : originalUnassigned) {
                if (isElearning(s)) {
                    finalToSave.add(s);
                } else if (bestAllocations.containsKey(s.getId())) {
                    ScheduleStateInfo info = bestAllocations.get(s.getId());
                    s.setRoom(info.room);
                    s.setDayOfWeek(info.dayOfWeek);
                    s.setStartPeriod(info.startPeriod);
                    s.setEndPeriod(info.endPeriod);
                    finalToSave.add(s);
                } else if (!pinnedScheduleIds.contains(s.getId())) {
                    if (s.getRoom() != null || (s.getDayOfWeek() != null && s.getDayOfWeek() != 8)) {
                        resetScheduleState(s);
                    }
                }
            }

            try {
                if (!finalToSave.isEmpty()) scheduleRepository.saveAll(finalToSave);
            } catch (Exception e) {
                log.error("[Greedy] DB ERROR", e);
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi DB: " + e.getMessage());
            }

            log.info("[Greedy] FINAL RESULT: {:.1f}% Success", bestResult.getSuccessRate());
            return bestResult;
        }
        //Thử xếp lịch cho 1 schedule
        private String tryPlace(Schedule schedule, Map<UUID, List<Room>> roomsByType, List<Room> allRooms,
                                SchedulingContext ctx, Set<UUID> pinnedScheduleIds,
                                List<Schedule> allSemesterSchedules, Map<UUID, List<ClassSection>> childrenCache) {
            //Nếu lịch đã được cố định thì bỏ qua, coi như đã xếp thành công
            if (pinnedScheduleIds.contains(schedule.getId())) return SUCCESS;
            //Lấy thông tin về số tiết và phòng,sức chứa yêu caafu
            SubjectComponent comp = schedule.getClassSection().getSubjectComponent();
            //Nếu không có thông tin thì xếp mặc định
            int periods = (comp != null && comp.getPeriodsPerSession() != null) ? comp.getPeriodsPerSession() : 3;
            RoomType reqType = comp != null ? comp.getRequiredRoomType() : null;
            int capacity = Optional.ofNullable(schedule.getClassSection().getCapacity()).orElse(0);
            //Lấy môn học để kiểm tra số lớp cùng môn đã xếp vào slot đó có vượt giới hạn maxConcurrent hay không
            UUID subjectId = Optional.ofNullable(schedule.getClassSection().getSubject())
                    .map(Subject::getId).orElse(null);
            //Xác định danh sách phòng cơ bản để xếp dựa trên yêu cầu loại phòng, nếu không yêu cầu thì dùng toàn bộ phòng
            List<Room> baseRooms = new ArrayList<>();
            if (reqType == null) {
                baseRooms = allRooms;
            } else {
                baseRooms.addAll(roomsByType.getOrDefault(reqType.getId(), Collections.emptyList()));
                if (baseRooms.isEmpty() && "THEORY".equals(reqType.getCode())) {
                    baseRooms = allRooms;
                }
            }

            if (baseRooms.isEmpty()) {
                return ERR_NO_ROOM;
            }
            // Sinh ra danh sách các các thời điểm bắt đầu hợp lệ
            List<Integer> validStarts = (periods <= 5)
                    ? VALID_STARTS_CACHE.computeIfAbsent(periods, this::generateValidStartsFallback)
                    : generateValidStartsFallback(periods);

            int roomBusyFails = 0;
            int capacityLimitFails = 0;
            int parentChildFails = 0;
            // Randomize thứ tự ngày và thời điểm bắt đầu để tăng cơ hội tìm được slot trống, tránh xếp theo cùng 1 pattern cứng nhắc
            List<Integer> randomizedDays = new ArrayList<>(Arrays.asList(2, 3, 4, 5, 6, 7));
            Collections.shuffle(randomizedDays);
            // Randomize thứ tự thời điểm bắt đầu
            List<Integer> randomizedStarts = new ArrayList<>(validStarts);
            Collections.shuffle(randomizedStarts);

            for (int start : randomizedStarts) {
                for (int day : randomizedDays) {
                    int end = start + periods - 1;
                    if (end > MAX_PERIOD) continue;

                    if (!allowCrossLunch && !isValidBlock(start, end)) continue;
                    // Kiểm tra giới hạn số lớp cùng môn học đã xếp vào slot này
                    if (subjectId != null && !checkCapacity(ctx, subjectId, day, start, end)) {
                        capacityLimitFails++;
                        continue;
                    }
                    // Kiểm tra xung đột lịch với lớp parent hoặc các lớp child
                    boolean hasParentChildConflict = false;
                    ClassSection currentSection = schedule.getClassSection();

                    if (currentSection.getParentSection() != null) {
                        hasParentChildConflict = isConflictWithParent(currentSection.getParentSection(), day, start, end, allSemesterSchedules);
                    } else {
                        List<ClassSection> childSections = childrenCache.getOrDefault(currentSection.getId(), Collections.emptyList());
                        hasParentChildConflict = isConflictWithAnyChild(childSections, day, start, end, allSemesterSchedules);
                    }

                    if (hasParentChildConflict) {
                        parentChildFails++;
                        continue;
                    }
                    // Kiểm tra giáo viên có bận dạy lớp khác vào slot này hay không
                    boolean isTeacherBusy = false;
                    if (schedule.getLecturer() != null) {
                        for (int p = start; p <= end; p++) {
                            if (ctx.isLecturerBusy(schedule.getLecturer().getId(), day, p)) {
                                isTeacherBusy = true;
                                break;
                            }
                        }
                    }
                    if (isTeacherBusy) continue;
                    // Randomize thứ tự phòng để tránh xếp theo cùng 1 pattern cứng nhắc, tăng cơ hội tìm được slot trống
                    List<Room> randomizedRooms = new ArrayList<>(baseRooms);
                    Collections.shuffle(randomizedRooms);

                    for (Room room : randomizedRooms) {
                        if (room.getCapacity() == null || room.getCapacity() < capacity) continue;

                        if (!isRoomFree(ctx, room.getId(), day, start, end)) {
                            roomBusyFails++;
                            continue;
                        }

                        ctx.setRoomBusy(room.getId(), day, start, end, true);
                        if (subjectId != null) ctx.updateSubjectConcurrent(subjectId, day, start, end, +1);

                        if (schedule.getLecturer() != null) {
                            ctx.setLecturerBusy(schedule.getLecturer().getId(), day, start, end, true);
                        }

                        schedule.setRoom(room);
                        schedule.setDayOfWeek(day);
                        schedule.setStartPeriod(start);
                        schedule.setEndPeriod(end);
                        return SUCCESS;
                    }
                }
            }

            int totalFails = roomBusyFails + capacityLimitFails + parentChildFails;
            if (totalFails == 0) return ERR_SLOT;

            if (parentChildFails > roomBusyFails && parentChildFails > capacityLimitFails) return ERR_PARENT_CHILD_CONFLICT;

            double capacityRatio = capacityLimitFails * 1.0 / totalFails;
            if (capacityRatio > 0.5) return ERR_CAPACITY;

            return ERR_NO_ROOM;
        }
        //Kiểm tra 2  lịch có trùng ngày và giờ hay không
        private boolean checkTimeOverlap(Schedule s1, Integer testDay, Integer testStart, Integer testEnd) {
            if (s1.getDayOfWeek() == null || s1.getStartPeriod() == null || s1.getEndPeriod() == null) return false;
            if (!s1.getDayOfWeek().equals(testDay)) return false;
            return s1.getStartPeriod() <= testEnd && s1.getEndPeriod() >= testStart;
        }
        //Kiểm tra lịch của parent có trùng với slot đang xét hay không
        private boolean isConflictWithParent(ClassSection parentSection, Integer slotDay, Integer slotStart, Integer slotEnd, List<Schedule> allSemesterSchedules) {
            return allSemesterSchedules.stream()
                    .filter(s -> s.getClassSection().getId().equals(parentSection.getId()))
                    .anyMatch(parentSchedule -> checkTimeOverlap(parentSchedule, slotDay, slotStart, slotEnd));
        }
        //Kiểm tra lịch của tất cả child có trùng với slot đang xét hay không
        private boolean isConflictWithAnyChild(List<ClassSection> childSections, Integer slotDay, Integer slotStart, Integer slotEnd, List<Schedule> allSemesterSchedules) {
            if (childSections == null || childSections.isEmpty()) return false;
            Set<UUID> childIds = childSections.stream().map(ClassSection::getId).collect(Collectors.toSet());
            return allSemesterSchedules.stream()
                    .filter(s -> childIds.contains(s.getClassSection().getId()))
                    .anyMatch(childSchedule -> checkTimeOverlap(childSchedule, slotDay, slotStart, slotEnd));
        }

        private List<Integer> generateValidStartsFallback(int periods) {
            List<Integer> all = new ArrayList<>();
            for (int i = 1; i <= MAX_PERIOD - periods + 1; i++) all.add(i);
            return Collections.unmodifiableList(all);
        }
            // Kiểm tra phòng có trống trong khoảng thời gian đã xét hay không
        private boolean isRoomFree(SchedulingContext ctx, UUID roomId, int day, int start, int end) {
            for (int p = start; p <= end; p++) {
                if (ctx.isRoomBusy(roomId, day, p)) return false;
            }
            return true;
        }
        //Kiểm tra số lớp cùng môn học đã được xếp vào slot đó đã đạt giới hạn maxConcurrent hay chưa
        private boolean checkCapacity(SchedulingContext ctx, UUID subjectId, int day, int start, int end) {
            int max = ctx.getMaxConcurrentBySubject().getOrDefault(subjectId, Integer.MAX_VALUE);
            for (int p = start; p <= end; p++) {
                if (ctx.getSubjectConcurrent(subjectId, day, p) >= max) return false;
            }
            return true;
        }
        // Reset lại thông tin phòng và thời gian
        private void resetScheduleState(Schedule schedule) {
            schedule.setRoom(null);
            schedule.setDayOfWeek(null);
            schedule.setStartPeriod(null);
            schedule.setEndPeriod(null);
        }
        // Kiểm tra lịch có rơi vào block 1-5 và 6-10 hay không
        private boolean isValidBlock(int start, int end) {
            return !(start <= 5 && end >= 6);
        }
        // Tính số lượng phòng đủ điều cho 1 schedule
        private int getEligibleRoomCount(Schedule s, Map<UUID, List<Room>> roomsByType, List<Room> allRooms) {
            RoomType reqType = s.getClassSection().getSubjectComponent() != null
                    ? s.getClassSection().getSubjectComponent().getRequiredRoomType() : null;
            //Lấy sức chứa của lớp học phần, nếu null thì coi như 0 để tránh lỗi
            int cap = Optional.ofNullable(s.getClassSection().getCapacity()).orElse(0);
            //Nếu không yêu cầu thì toàn bộ phòng
            List<Room> base = new ArrayList<>();
            if (reqType == null) {
                base = allRooms;
            } else {
                base.addAll(roomsByType.getOrDefault(reqType.getId(), Collections.emptyList()));
                if (base.isEmpty() && "THEORY".equals(reqType.getCode())) {
                    base = allRooms;
                }
            }

            return (int) base.stream().filter(r -> r.getCapacity() != null && r.getCapacity() >= cap).count();
        }
            //Lý do không xếp được lịch
        private AutoAssignResultResponse.FailedScheduleInfo buildFailedInfo(Schedule schedule, String reason) {
            return AutoAssignResultResponse.FailedScheduleInfo.builder()
                    .scheduleId(schedule.getId())
                    .sectionCode(schedule.getClassSection().getSectionCode())
                    .subjectName(schedule.getClassSection().getSubject() != null ? schedule.getClassSection().getSubject().getName() : "Unknown")
                    .reason(reason)
                    .build();
        }

        @lombok.Data
        @lombok.AllArgsConstructor
        private static class ScheduleStateInfo {
            private Room room;
            private Integer dayOfWeek;
            private Integer startPeriod;
            private Integer endPeriod;
        }
    }