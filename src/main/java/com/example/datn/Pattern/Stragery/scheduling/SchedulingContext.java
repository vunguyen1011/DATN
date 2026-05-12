package com.example.datn.Pattern.Stragery.scheduling;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 1 – Data container chứa toàn bộ in-memory matrices cho 1 lần chạy scheduling.
 *
 * SỬ DỤNG DIRECT INDEXING (Không cần trừ Offset):
 * - day   : 2 (Thứ 2) → 8 (Chủ nhật) → matrix[2] tới matrix[8]
 * - period: 1 → 16                   → matrix[1] tới matrix[16] (Hỗ trợ E-learning)
 */
@Getter
public class SchedulingContext {

    // Nới rộng kích thước mảng để dùng trực tiếp Index thực tế.
    public static final int DAY_COUNT    = 9;   // Index 0..8 (Dùng 2..8)
    public static final int PERIOD_COUNT = 17;  // Index 0..16 (Dùng 1..16)

    private final UUID semesterId;

    /** roomId → [day][period] = true nếu phòng bận */
    private final Map<UUID, boolean[][]> roomBusy = new HashMap<>();

    /** lecturerId → [day][period] = true nếu GV bận */
    private final Map<UUID, boolean[][]> lecturerBusy = new HashMap<>();

    /** subjectId → [day][period] = số lớp môn đó đang mở cùng lúc. */
    private final Map<UUID, int[][]> subjectConcurrentMatrix = new HashMap<>();

    /** subjectId → số lớp tối đa có thể mở song song */
    private final Map<UUID, Integer> maxConcurrentBySubject = new HashMap<>();

    /** lecturerId → tổng số tiết hiện đang được phân công trong semester */
    private final Map<UUID, Integer> lecturerCurrentLoad = new HashMap<>();

    public SchedulingContext(UUID semesterId) {
        this.semesterId = semesterId;
    }

    // ── Helper: Dynamic Safe Bounds Check ─────────────────────────────────────

    private boolean isOutOfBounds(int day, int period) {
        return day < 2 || day >= DAY_COUNT || period < 1 || period >= PERIOD_COUNT;
    }

    // ── Room matrix helpers ───────────────────────────────────────────────────

    public boolean isRoomBusy(UUID roomId, int dayOfWeek, int period) {
        if (isOutOfBounds(dayOfWeek, period)) return true;

        boolean[][] matrix = roomBusy.get(roomId);
        if (matrix == null) return false;
        return matrix[dayOfWeek][period];
    }

    public void setRoomBusy(UUID roomId, int dayOfWeek, int startPeriod, int endPeriod, boolean busy) {
        if (isOutOfBounds(dayOfWeek, startPeriod) || isOutOfBounds(dayOfWeek, endPeriod)) return;

        roomBusy.computeIfAbsent(roomId, k -> new boolean[DAY_COUNT][PERIOD_COUNT]);
        boolean[][] m = roomBusy.get(roomId);
        for (int p = startPeriod; p <= endPeriod; p++) {
            m[dayOfWeek][p] = busy;
        }
    }

    // ── Lecturer matrix helpers ───────────────────────────────────────────────

    public boolean isLecturerBusy(UUID lecturerId, int dayOfWeek, int period) {
        if (isOutOfBounds(dayOfWeek, period)) return true;

        boolean[][] matrix = lecturerBusy.get(lecturerId);
        if (matrix == null) return false;
        return matrix[dayOfWeek][period];
    }

    public void setLecturerBusy(UUID lecturerId, int dayOfWeek, int startPeriod, int endPeriod, boolean busy) {
        if (isOutOfBounds(dayOfWeek, startPeriod) || isOutOfBounds(dayOfWeek, endPeriod)) return;

        lecturerBusy.computeIfAbsent(lecturerId, k -> new boolean[DAY_COUNT][PERIOD_COUNT]);
        boolean[][] m = lecturerBusy.get(lecturerId);
        for (int p = startPeriod; p <= endPeriod; p++) {
            m[dayOfWeek][p] = busy;
        }
    }

    public boolean hasLecturerClassOnDay(UUID lecturerId, int dayOfWeek) {
        if (dayOfWeek < 2 || dayOfWeek >= DAY_COUNT) return false;

        boolean[][] matrix = lecturerBusy.get(lecturerId);
        if (matrix == null) return false;
        for (int p = 1; p < PERIOD_COUNT; p++) {
            if (matrix[dayOfWeek][p]) return true;
        }
        return false;
    }

    // ── Subject concurrent matrix helpers ────────────────────────────────────

    public int getSubjectConcurrent(UUID subjectId, int dayOfWeek, int period) {
        if (isOutOfBounds(dayOfWeek, period)) return 999;

        int[][] matrix = subjectConcurrentMatrix.get(subjectId);
        if (matrix == null) return 0;
        return matrix[dayOfWeek][period];
    }

    public void updateSubjectConcurrent(UUID subjectId, int dayOfWeek, int startPeriod, int endPeriod, int delta) {
        if (isOutOfBounds(dayOfWeek, startPeriod) || isOutOfBounds(dayOfWeek, endPeriod)) return;

        subjectConcurrentMatrix.computeIfAbsent(subjectId, k -> new int[DAY_COUNT][PERIOD_COUNT]);
        int[][] m = subjectConcurrentMatrix.get(subjectId);
        for (int p = startPeriod; p <= endPeriod; p++) {
            m[dayOfWeek][p] += delta;
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