package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProgramTreeResponse {
    // --- RỄ CÂY: THÔNG TIN KHUNG CHƯƠNG TRÌNH ---
    private UUID id;
    private String programName;
    private Integer minTotalCredits;
    private Float durationYears;

    // Danh sách các Khối Hồng lồng bên trong
    private List<GroupNode> groups;

    // --- NHÁNH 1: KHỐI MÀU HỒNG (SubjectGroup) ---
    @Data
    @Builder
    public static class GroupNode {
        private UUID id;
        private String name;
        private int index; // Vị trí hiển thị của Khối Hồng trong cây
        private Boolean isGlobal;

        // Danh sách các Đoạn Cam lồng bên trong
        private List<SectionNode> sections;
    }

    // --- NHÁNH 2: ĐOẠN MÀU CAM (SubjectGroupSection) ---
    @Data
    @Builder
    public static class SectionNode {
        private UUID id;
        private String title;
        private Boolean isMandatory; // true: Bắt buộc, false: Tự chọn
        private Integer requiredCredits;

        // Danh sách các Môn Học lồng bên trong
        private List<SubjectNode> subjects;
    }

    // --- LÁ CÂY: MÔN HỌC (Subject) ---
    @Data
    @Builder
    public static class SubjectNode {
        private UUID id; // ID của bảng ProgramSubject
        private UUID subjectId; // ID của môn học gốc trong bảng Subject
        private String subjectCode;
        private String subjectName;
        private Integer credits;

        // Các thông tin chi tiết mới bổ sung
        private String departmentName;
        private Integer theoryPeriod;
        private Integer practicePeriod;
    }
}