package com.example.datn.Pattern.Stragery.scheduling;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 1 – Data container chứa toàn bộ in-memory matrices cho 1 lần chạy scheduling.
 *
 * <ul>
 *   <li>day  : 2 (Thứ 2) → 8 (Chủ nhật) → index [day - 2]</li>
 *   <li>period: 1 → 15   → index [period - 1]</li>
 * </ul>
 *
 * Tất cả dữ liệu được scope theo semesterId để không lẫn giữa các học kỳ.
 */
@Getter
public class SchedulingContext {

    public static final int DAY_COUNT    = 7;   // Thứ 2 → Chủ nhật (index 0-6)
    public static final int PERIOD_COUNT = 15;  // Tiết 1 → 15    (index 0-14)
    private final UUID semesterId;

    /** roomId → [day-index][period-index] = true nếu phòng bận */
    private final Map<UUID, boolean[][]> roomBusy = new HashMap<>();

    /** lecturerId → [day-index][period-index] = true nếu GV bận */
    private final Map<UUID, boolean[][]> lecturerBusy = new HashMap<>();

    /**
     * subjectId → [day-index][period-index] = số lớp môn đó đang mở cùng lúc.
     * Dùng cho Look-Ahead Capacity check.
     */
    private final Map<UUID, int[][]> subjectConcurrentMatrix = new HashMap<>();

    /**
     * subjectId → số lớp tối đa có thể mở song song
     * = số GV có thể dạy môn đó trong kỳ.
     */
    private final Map<UUID, Integer> maxConcurrentBySubject = new HashMap<>();

    /** lecturerId → tổng số tiết hiện đang được phân công trong semester */
    private final Map<UUID, Integer> lecturerCurrentLoad = new HashMap<>();

    public SchedulingContext(UUID semesterId) {
        this.semesterId = semesterId;
    }

    // ── Helper: convert day/period sang array index ───────────────────────────

    /** dayOfWeek (2–8) → array index (0–6) */
    public static int dayIdx(int dayOfWeek) {
        return dayOfWeek - 2;
    }

    /** period (1–15) → array index (0–14) */
    public static int periodIdx(int period) {
        return period - 1;
    }

    // ── Room matrix helpers ───────────────────────────────────────────────────

    public boolean isRoomBusy(UUID roomId, int dayOfWeek, int period) {
        boolean[][] matrix = roomBusy.get(roomId);
        if (matrix == null) return false;
        return matrix[dayIdx(dayOfWeek)][periodIdx(period)];
    }

    public void setRoomBusy(UUID roomId, int dayOfWeek, int startPeriod, int endPeriod, boolean busy) {
        roomBusy.computeIfAbsent(roomId, k -> new boolean[DAY_COUNT][PERIOD_COUNT]);
        boolean[][] m = roomBusy.get(roomId);
        for (int p = startPeriod; p <= endPeriod; p++) {
            m[dayIdx(dayOfWeek)][periodIdx(p)] = busy;
        }
    }

    // ── Lecturer matrix helpers ───────────────────────────────────────────────

    public boolean isLecturerBusy(UUID lecturerId, int dayOfWeek, int period) {
        boolean[][] matrix = lecturerBusy.get(lecturerId);
        if (matrix == null) return false;
        return matrix[dayIdx(dayOfWeek)][periodIdx(period)];
    }

    public void setLecturerBusy(UUID lecturerId, int dayOfWeek, int startPeriod, int endPeriod, boolean busy) {
        lecturerBusy.computeIfAbsent(lecturerId, k -> new boolean[DAY_COUNT][PERIOD_COUNT]);
        boolean[][] m = lecturerBusy.get(lecturerId);
        for (int p = startPeriod; p <= endPeriod; p++) {
            m[dayIdx(dayOfWeek)][periodIdx(p)] = busy;
        }
    }

    public boolean hasLecturerClassOnDay(UUID lecturerId, int dayOfWeek) {
        boolean[][] matrix = lecturerBusy.get(lecturerId);
        if (matrix == null) return false;
        for (int p = 0; p < PERIOD_COUNT; p++) {
            if (matrix[dayIdx(dayOfWeek)][p]) return true;
        }
        return false;
    }

    // ── Subject concurrent matrix helpers ────────────────────────────────────

    public int getSubjectConcurrent(UUID subjectId, int dayOfWeek, int period) {
        int[][] matrix = subjectConcurrentMatrix.get(subjectId);
        if (matrix == null) return 0;
        return matrix[dayIdx(dayOfWeek)][periodIdx(period)];
    }

    public void updateSubjectConcurrent(UUID subjectId, int dayOfWeek, int startPeriod, int endPeriod, int delta) {
        subjectConcurrentMatrix.computeIfAbsent(subjectId, k -> new int[DAY_COUNT][PERIOD_COUNT]);
        int[][] m = subjectConcurrentMatrix.get(subjectId);
        for (int p = startPeriod; p <= endPeriod; p++) {
            m[dayIdx(dayOfWeek)][periodIdx(p)] += delta;
        }
    }

    // ── Lecturer load helpers ─────────────────────────────────────────────────

    public int getLecturerLoad(UUID lecturerId) {
        return lecturerCurrentLoad.getOrDefault(lecturerId, 0);
    }

    public void addLecturerLoad(UUID lecturerId, int periods) {
        lecturerCurrentLoad.merge(lecturerId, periods, Integer::sum);
    }

    public void subtractLecturerLoad(UUID lecturerId, int periods) {
        lecturerCurrentLoad.merge(lecturerId, -periods, Integer::sum);
    }
}
