package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TemplateTreeResponse {
    private List<GroupNode> groups;

    @Data @Builder
    public static class GroupNode {
        private UUID id;
        private String name;
        private Integer index;
        private List<SectionNode> sections;
    }

    @Data @Builder
    public static class SectionNode {
        private UUID id;
        private String title;
        private Boolean isMandatory;
        private Integer requiredCredits;
        private Integer index;
        private List<SubjectNode> subjects;
    }

    @Data @Builder
    public static class SubjectNode {
        private UUID id; // ID của bảng SectionDefaultSubject
        private UUID subjectId;
        private String subjectName;
        private String subjectCode;
        private Integer credits;
        private Integer defaultSemester;
    }
}