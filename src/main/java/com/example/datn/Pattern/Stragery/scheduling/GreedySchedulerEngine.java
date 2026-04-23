package com.example.datn.Pattern.Stragery.scheduling;

import com.example.datn.DTO.Response.AutoAssignResultResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.*;
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
    private static final int MAX_PERIOD = 15;
    private static final UUID NULL_TYPE_KEY = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String ELEARNING_TYPE_ID = "f6a7b8c9-d0e1-2f3a-4b5c-6d7e8f9a0b1c";

    public static final String SUCCESS = "SUCCESS";
    public static final String ERR_NO_ROOM = "NO_ROOM_AVAILABLE";
    public static final String ERR_CAPACITY = "TEACHER_CAPACITY_LIMIT";
    public static final String ERR_SLOT = "NO_VALID_SLOT";

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

    private boolean isElearning(Schedule s) {
        SubjectComponent comp = s.getClassSection().getSubjectComponent();
        return comp != null && comp.getRequiredRoomType() != null
                && comp.getRequiredRoomType().getId().toString().equals(ELEARNING_TYPE_ID);
    }

    @Transactional
    public AutoAssignResultResponse run(UUID semesterId) {
        log.info("[Greedy] === START Production RC Auto-Schedule semester {} ===", semesterId);

        List<Room> allRooms = roomRepository.findAll();
        allRooms.sort(Comparator.comparingInt(r -> r.getCapacity() == null ? 0 : r.getCapacity()));

        Map<UUID, List<Room>> roomsByType = new HashMap<>();
        for (Room r : allRooms) {
            UUID key = (r.getRoomType() == null) ? NULL_TYPE_KEY : r.getRoomType().getId();
            roomsByType.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        roomsByType.values().forEach(list ->
                list.sort(Comparator.comparingInt(r -> r.getCapacity() == null ? 0 : r.getCapacity()))
        );

        List<Schedule> originalUnassigned = scheduleRepository.findUnassignedSchedulesWithSection(semesterId);

        // VẤN ĐỀ 1: Fake slot cho E-learning để trả về DB nhưng bóc ra khỏi engine
        for (Schedule s : originalUnassigned) {
            if (isElearning(s)) {
                s.setRoom(null);
                s.setDayOfWeek(8);
                s.setStartPeriod(10);
                s.setEndPeriod(12);
            }
        }

        // Loại bỏ hoàn toàn E-learning khỏi danh sách lập lịch của Greedy Engine
        List<Schedule> schedulingList = originalUnassigned.stream()
                .filter(s -> !isElearning(s))
                .collect(Collectors.toList());

        Set<UUID> pinnedScheduleIds = schedulingList.stream()
                .filter(s -> s.getDayOfWeek() != null && s.getStartPeriod() != null)
                .map(Schedule::getId)
                .collect(Collectors.toSet());

        Map<UUID, Integer> eligibleRoomCache = new HashMap<>();
        for (Schedule s : schedulingList) {
            if (!pinnedScheduleIds.contains(s.getId())) {
                eligibleRoomCache.put(s.getId(), getEligibleRoomCount(s, roomsByType, allRooms));
            }
        }

        int maxAttempts = 2;
        AutoAssignResultResponse bestResult = null;
        Map<UUID, ScheduleStateInfo> bestAllocations = new HashMap<>();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.info("[Greedy] Running Attempt {}/{}", attempt, maxAttempts);

            List<Schedule> unassigned = new ArrayList<>(schedulingList);

            if (attempt > 1) {
                unassigned.forEach(s -> {
                    if (!pinnedScheduleIds.contains(s.getId())) resetScheduleState(s);
                });
            }

            if (attempt == 1) {
                unassigned.sort(Comparator
                        .comparingInt((Schedule s) -> eligibleRoomCache.getOrDefault(s.getId(), 999))
                        .thenComparing(Comparator.comparingInt(
                                (Schedule s) -> Optional.ofNullable(s.getClassSection().getCapacity()).orElse(0)
                        ).reversed())
                );
            } else {
                Collections.shuffle(unassigned);
            }

            SchedulingContext ctx = matrixBuilder.build(semesterId);

            for (Schedule s : unassigned) {
                if (pinnedScheduleIds.contains(s.getId())) {
                    UUID subjId = Optional.ofNullable(s.getClassSection().getSubject()).map(Subject::getId).orElse(null);

                    if (s.getRoom() != null) {
                        ctx.setRoomBusy(s.getRoom().getId(), s.getDayOfWeek(), s.getStartPeriod(), s.getEndPeriod(), true);
                    }
                    if (subjId != null) {
                        ctx.updateSubjectConcurrent(subjId, s.getDayOfWeek(), s.getStartPeriod(), s.getEndPeriod(), +1);
                    }
                    if (s.getLecturer() != null) {
                        // Dùng matrix chung thay vì HashSet tạm
                        ctx.setLecturerBusy(s.getLecturer().getId(), s.getDayOfWeek(), s.getStartPeriod(), s.getEndPeriod(), true);
                    }
                }
            }

            List<Schedule> toSave = new ArrayList<>();
            List<AutoAssignResultResponse.FailedScheduleInfo> failed = new ArrayList<>();

            for (Schedule schedule : unassigned) {
                if (!pinnedScheduleIds.contains(schedule.getId()) && eligibleRoomCache.getOrDefault(schedule.getId(), 0) == 0) {
                    failed.add(buildFailedInfo(schedule, ERR_NO_ROOM));
                    continue;
                }

                String result = tryPlace(schedule, roomsByType, allRooms, ctx, pinnedScheduleIds);

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

    private String tryPlace(Schedule schedule, Map<UUID, List<Room>> roomsByType, List<Room> allRooms, SchedulingContext ctx, Set<UUID> pinnedScheduleIds) {

        if (pinnedScheduleIds.contains(schedule.getId())) return SUCCESS;

        SubjectComponent comp = schedule.getClassSection().getSubjectComponent();
        int periods = (comp != null && comp.getPeriodsPerSession() != null) ? comp.getPeriodsPerSession() : 3;
        RoomType reqType = comp != null ? comp.getRequiredRoomType() : null;
        int capacity = Optional.ofNullable(schedule.getClassSection().getCapacity()).orElse(0);

        UUID subjectId = Optional.ofNullable(schedule.getClassSection().getSubject())
                .map(Subject::getId).orElse(null);

        // VẤN ĐỀ 2: Mở rộng Fallback logic cho THEORY -> có thể học ANY (cả Practice)
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

        List<Integer> validStarts = (periods <= 5)
                ? VALID_STARTS_CACHE.computeIfAbsent(periods, this::generateValidStartsFallback)
                : generateValidStartsFallback(periods);

        int roomBusyFails = 0;
        int capacityLimitFails = 0;

        List<Integer> randomizedDays = new ArrayList<>(Arrays.asList(2, 3, 4, 5, 6, 7));
        Collections.shuffle(randomizedDays);

        List<Integer> randomizedStarts = new ArrayList<>(validStarts);
        Collections.shuffle(randomizedStarts);

        for (int start : randomizedStarts) {
            for (int day : randomizedDays) {
                int end = start + periods - 1;
                if (end > MAX_PERIOD) continue;

                if (!allowCrossLunch && !isValidBlock(start, end)) continue;

                if (subjectId != null && !checkCapacity(ctx, subjectId, day, start, end)) {
                    capacityLimitFails++;
                    continue;
                }

                // VẤN ĐỀ 3: Check lecturer thông qua Matrix tập trung
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

        int totalFails = roomBusyFails + capacityLimitFails;
        if (totalFails == 0) return ERR_SLOT;

        double capacityRatio = capacityLimitFails * 1.0 / totalFails;
        if (capacityRatio > 0.5) return ERR_CAPACITY;

        return ERR_NO_ROOM;
    }

    private List<Integer> generateValidStartsFallback(int periods) {
        List<Integer> all = new ArrayList<>();
        for (int i = 1; i <= MAX_PERIOD - periods + 1; i++) all.add(i);
        return Collections.unmodifiableList(all);
    }

    private boolean isRoomFree(SchedulingContext ctx, UUID roomId, int day, int start, int end) {
        for (int p = start; p <= end; p++) {
            if (ctx.isRoomBusy(roomId, day, p)) return false;
        }
        return true;
    }

    private boolean checkCapacity(SchedulingContext ctx, UUID subjectId, int day, int start, int end) {
        int max = ctx.getMaxConcurrentBySubject().getOrDefault(subjectId, Integer.MAX_VALUE);
        for (int p = start; p <= end; p++) {
            if (ctx.getSubjectConcurrent(subjectId, day, p) >= max) return false;
        }
        return true;
    }

    private void resetScheduleState(Schedule schedule) {
        schedule.setRoom(null);
        schedule.setDayOfWeek(null);
        schedule.setStartPeriod(null);
        schedule.setEndPeriod(null);
    }

    private boolean isValidBlock(int start, int end) {
        return !(start <= 5 && end >= 6);
    }

    private int getEligibleRoomCount(Schedule s, Map<UUID, List<Room>> roomsByType, List<Room> allRooms) {
        RoomType reqType = s.getClassSection().getSubjectComponent() != null
                ? s.getClassSection().getSubjectComponent().getRequiredRoomType() : null;
        int cap = Optional.ofNullable(s.getClassSection().getCapacity()).orElse(0);

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