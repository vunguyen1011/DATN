package com.example.datn.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    private List<SubjectRecommendation> recommendedSubjects;
    private String explanation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectRecommendation {
        private UUID subjectId;
        private String subjectName;
        private String subjectCode;
        private Integer score;
        private String reason;
    }
}
