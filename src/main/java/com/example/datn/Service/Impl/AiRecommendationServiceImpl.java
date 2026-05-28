package com.example.datn.Service.Impl;

import com.example.datn.DTO.Response.RecommendationResponse;
import com.example.datn.ENUM.RecommendationStatus;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IAiRecommendationService;
import com.example.datn.Service.Interface.IGeminiService;
import com.example.datn.Service.Interface.IRedisService;
import com.example.datn.Util.HashUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationServiceImpl implements IAiRecommendationService {

    private final StudentRepository studentRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final ProgramSubjectRepository programSubjectRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final AiRecommendationHistoryRepository aiRecommendationHistoryRepository;
    private final IRedisService redisService;
    private final SemesterRepository semesterRepository;
    private final IGeminiService geminiService;
    private final ObjectMapper objectMapper;

    private static final int MAX_RECOMMENDATIONS = 5;
    
    // Local lock map to prevent Cache Stampede per student
    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public RecommendationResponse getRecommendations(UUID studentId) {
        // Step 1: Redis Cache Check (Fastest Path)
        String cachedResult = redisService.getRecommendation(studentId.toString());
        if (cachedResult != null) {
            try {
                log.info("Lấy AI Recommendation từ Cache cho student: {}", studentId);
                return objectMapper.readValue(cachedResult, RecommendationResponse.class);
            } catch (Exception e) {
                log.error("Failed to parse cached recommendation", e);
            }
        }

        // Use lock per student to prevent multiple requests hitting DB/AI simultaneously
        Object lock = locks.computeIfAbsent(studentId, k -> new Object());
        synchronized (lock) {
            try {
                // Double check cache after acquiring lock
                cachedResult = redisService.getRecommendation(studentId.toString());
                if (cachedResult != null) {
                    return objectMapper.readValue(cachedResult, RecommendationResponse.class);
                }

                return processRecommendationLogic(studentId);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý Recommendation", e);
                // Return fallback if everything fails
                return getFallbackRecommendations(Collections.emptyList(), "Lỗi hệ thống. Vui lòng thử lại sau.");
            }
        }
    }

    private RecommendationResponse processRecommendationLogic(UUID studentId) throws JsonProcessingException {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Semester semester= semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        // Load academic state
        List<StudentGrade> rawGradingHistory = studentGradeRepository.findAllByStudentId(studentId);
        List<StudentGrade> gradingHistory = deduplicateGrades(rawGradingHistory);

        Set<UUID> passedSubjectIds = gradingHistory.stream()
                .filter(sg -> Boolean.TRUE.equals(sg.getIsPassed()))
                .map(sg -> sg.getEnrollment().getClassSection().getSubject().getId())
                .collect(Collectors.toSet());

        List<ProgramSubject> openedSubjects = programSubjectRepository.findSubjectsByCohortAndMajor(
                student.getCohort().getId(),
                student.getMajor().getId()
        );

        List<ProgramSubject> unpassedSubjects = openedSubjects.stream()
                .filter(ps -> !passedSubjectIds.contains(ps.getSubject().getId()))
                .collect(Collectors.toList());

        Set<UUID> unpassedSubjectIds = unpassedSubjects.stream()
                .map(ps -> ps.getSubject().getId())
                .collect(Collectors.toSet());

        List<Prerequisite> allPrerequisites = prerequisiteRepository.findBySubjectIdIn(unpassedSubjectIds);
        Map<UUID, List<Prerequisite>> prerequisiteMap = allPrerequisites.stream()
                .collect(Collectors.groupingBy(pre -> pre.getSubject().getId()));

        List<ProgramSubject> candidateSubjects = unpassedSubjects.stream()
                .filter(ps -> checkPrerequisites(ps.getSubject().getId(), passedSubjectIds, prerequisiteMap))
                .collect(Collectors.toList());

        if (candidateSubjects.isEmpty()) {
            RecommendationResponse emptyResp = RecommendationResponse.builder()
                    .explanation("Bạn không có môn học nào đủ điều kiện đăng ký lúc này.")
                    .recommendedSubjects(Collections.emptyList())
                    .build();
            cacheResult(studentId, emptyResp);
            return emptyResp;
        }

        double gpa = calculateWeightedGpa(gradingHistory);

        // Step 2 & 3: Compute Hash and Validate against DB Snapshot
        String currentHash = computeInputHash(studentId, gpa, gradingHistory, candidateSubjects);

        Optional<AiRecommendationHistory> dbSnapshotOpt = aiRecommendationHistoryRepository
                .findFirstByStudentIdAndStatusOrderByCreatedAtDesc(studentId, RecommendationStatus.ACTIVE);

        if (dbSnapshotOpt.isPresent()) {
            AiRecommendationHistory dbSnapshot = dbSnapshotOpt.get();
            if (currentHash.equals(dbSnapshot.getInputHash())) {
                log.info("Hash khớp. Tái sử dụng Recommendation từ DB Snapshot cho student: {}", studentId);
                RecommendationResponse response = objectMapper.readValue(dbSnapshot.getRecommendationJson(), RecommendationResponse.class);
                cacheResult(studentId, response);
                return response;
            } else {
                log.info("Hash lệch. Trạng thái học tập đã thay đổi. Deprecate bản ghi cũ.");
                aiRecommendationHistoryRepository.updateStatusByStudent(
                        studentId, RecommendationStatus.ACTIVE, RecommendationStatus.DEPRECATED);
            }
        }

        // Step 4: AI Generation
        String prompt = buildPrompt(student, gpa, gradingHistory, candidateSubjects);
        String rawAiResponse = null;
        RecommendationResponse response;
        try {
            rawAiResponse = geminiService.callGeminiApi(prompt);
            response = parseAndMapResponse(rawAiResponse, candidateSubjects);
            if (response.getRecommendedSubjects().isEmpty()) {
                response = getFallbackRecommendations(candidateSubjects, "Hệ thống AI tạm thời không thể phân tích được gợi ý tối ưu. Đây là lộ trình mặc định cho bạn.");
            }
        } catch (Exception e) {
            log.error("AI Recommendation failed or blocked. Triggering fallback.", e);
            response = getFallbackRecommendations(candidateSubjects, "Kết nối AI bận hoặc không khả dụng. Hệ thống chuyển sang gợi ý môn học theo lộ trình mặc định.");
            rawAiResponse = "ERROR: " + e.getMessage();
        }

        // Step 5: Persistence
        AiRecommendationHistory newHistory = AiRecommendationHistory.builder()
                .student(student)
                .semester(semester)
                .inputHash(currentHash)
                .recommendationJson(objectMapper.writeValueAsString(response))
                .rawAiResponse(rawAiResponse)
                .status(RecommendationStatus.ACTIVE)
                .aiModel("gemini-1.5-flash")
                .build();
        aiRecommendationHistoryRepository.save(newHistory);

        cacheResult(studentId, response);
        return response;
    }

    private void cacheResult(UUID studentId, RecommendationResponse response) {
        try {
            String jsonToCache = objectMapper.writeValueAsString(response);
            redisService.saveRecommendation(studentId.toString(), jsonToCache, Duration.ofHours(24));
        } catch (Exception e) {
            log.error("Lỗi khi lưu Recommendation vào Cache", e);
        }
    }

    private String computeInputHash(UUID studentId, double gpa, List<StudentGrade> gradingHistory, List<ProgramSubject> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Student:").append(studentId).append("|");
        sb.append("GPA:").append(String.format("%.2f", gpa)).append("|");

        List<String> passed = gradingHistory.stream()
                .filter(sg -> Boolean.TRUE.equals(sg.getIsPassed()))
                .map(sg -> sg.getEnrollment().getClassSection().getSubject().getCode())
                .sorted()
                .collect(Collectors.toList());
        sb.append("Passed:").append(String.join(",", passed)).append("|");

        List<String> failed = gradingHistory.stream()
                .filter(sg -> !Boolean.TRUE.equals(sg.getIsPassed()) && sg.getTotalScore() != null)
                .map(sg -> sg.getEnrollment().getClassSection().getSubject().getCode())
                .sorted()
                .collect(Collectors.toList());
        sb.append("Failed:").append(String.join(",", failed)).append("|");

        List<String> candidateCodes = candidates.stream()
                .map(ps -> ps.getSubject().getCode())
                .sorted()
                .collect(Collectors.toList());
        sb.append("Candidates:").append(String.join(",", candidateCodes));

        return HashUtils.generateSha256(sb.toString());
    }

    private List<StudentGrade> deduplicateGrades(List<StudentGrade> history) {
        Map<UUID, StudentGrade> bestGrades = new HashMap<>();
        for (StudentGrade sg : history) {
            UUID subjectId = sg.getEnrollment().getClassSection().getSubject().getId();
            if (!bestGrades.containsKey(subjectId)) {
                bestGrades.put(subjectId, sg);
            } else {
                StudentGrade existing = bestGrades.get(subjectId);
                boolean isPassed = Boolean.TRUE.equals(sg.getIsPassed());
                boolean existingPassed = Boolean.TRUE.equals(existing.getIsPassed());
                
                if (isPassed && !existingPassed) {
                    bestGrades.put(subjectId, sg);
                } else if (isPassed == existingPassed) {
                    double currScore = sg.getTotalScore() != null ? sg.getTotalScore() : 0.0;
                    double extScore = existing.getTotalScore() != null ? existing.getTotalScore() : 0.0;
                    if (currScore > extScore) {
                        bestGrades.put(subjectId, sg);
                    }
                }
            }
        }
        return new ArrayList<>(bestGrades.values());
    }

    private double calculateWeightedGpa(List<StudentGrade> history) {
        double weightedScore = 0;
        int totalCredits = 0;
        for (StudentGrade sg : history) {
            Subject subject = sg.getEnrollment().getClassSection().getSubject();
            int credits = subject.getCredits() != null ? subject.getCredits() : 0;
            double score = sg.getTotalScore() != null ? sg.getTotalScore() : 0.0;
            
            if (credits > 0 && sg.getTotalScore() != null) {
                weightedScore += score * credits;
                totalCredits += credits;
            }
        }
        return totalCredits > 0 ? weightedScore / totalCredits : 0.0;
    }

    private boolean checkPrerequisites(UUID subjectId, Set<UUID> passedSubjectIds, Map<UUID, List<Prerequisite>> prerequisiteMap) {
        List<Prerequisite> reqs = prerequisiteMap.get(subjectId);
        if (reqs == null || reqs.isEmpty()) {
            return true;
        }
        for (Prerequisite req : reqs) {
            if (!passedSubjectIds.contains(req.getPrerequisiteSubject().getId())) {
                return false;
            }
        }
        return true;
    }

    private RecommendationResponse getFallbackRecommendations(List<ProgramSubject> candidateSubjects, String message) {
        if (candidateSubjects == null || candidateSubjects.isEmpty()) {
            return RecommendationResponse.builder()
                    .explanation(message)
                    .recommendedSubjects(Collections.emptyList())
                    .build();
        }
        
        List<RecommendationResponse.SubjectRecommendation> recommendations = candidateSubjects.stream()
                .sorted(Comparator.comparing(ps -> ps.getSemester() != null ? ps.getSemester() : 99))
                .limit(MAX_RECOMMENDATIONS)
                .map(ps -> RecommendationResponse.SubjectRecommendation.builder()
                        .subjectId(ps.getSubject().getId())
                        .subjectCode(ps.getSubject().getCode())
                        .subjectName(ps.getSubject().getName())
                        .score(80)
                        .reason("Môn học bắt buộc theo lộ trình học kỳ tiếp theo.")
                        .build())
                .collect(Collectors.toList());

        return RecommendationResponse.builder()
                .explanation(message)
                .recommendedSubjects(recommendations)
                .build();
    }

    private String buildPrompt(Student student, double gpa, List<StudentGrade> gradingHistory, List<ProgramSubject> candidateSubjects) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là cố vấn học tập đại học chuyên nghiệp.\n\n");
        prompt.append("Thông tin sinh viên:\n");
        prompt.append("- Ngành học: ").append(student.getMajor().getName()).append("\n");
        prompt.append("- Điểm trung bình tích lũy (GPA hệ 10): ").append(String.format("%.2f", gpa)).append("\n\n");

        List<StudentGrade> failedSubjects = gradingHistory.stream()
                .filter(sg -> !Boolean.TRUE.equals(sg.getIsPassed()) && sg.getTotalScore() != null)
                .collect(Collectors.toList());
        
        List<StudentGrade> topPassedSubjects = gradingHistory.stream()
                .filter(sg -> Boolean.TRUE.equals(sg.getIsPassed()) && sg.getTotalScore() != null)
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .limit(5)
                .collect(Collectors.toList());

        if (!failedSubjects.isEmpty()) {
            prompt.append("Các môn sinh viên từng rớt (Cần ưu tiên học lại):\n");
            for (StudentGrade grade : failedSubjects) {
                Subject sub = grade.getEnrollment().getClassSection().getSubject();
                prompt.append("  + ").append(sanitizeText(sub.getName())).append(": ").append(grade.getTotalScore()).append("\n");
            }
        }

        if (!topPassedSubjects.isEmpty()) {
            prompt.append("\nTop 5 môn sinh viên học tốt nhất (Để đánh giá thế mạnh):\n");
            for (StudentGrade grade : topPassedSubjects) {
                Subject sub = grade.getEnrollment().getClassSection().getSubject();
                prompt.append("  + ").append(sanitizeText(sub.getName())).append(": ").append(grade.getTotalScore()).append("\n");
            }
        }

        prompt.append("\nDanh sách các môn học có thể đăng ký trong học kỳ tới:\n");
        for (ProgramSubject ps : candidateSubjects) {
            Subject sub = ps.getSubject();
            prompt.append("- [Mã: ").append(sub.getCode()).append("] ")
                  .append(sanitizeText(sub.getName()))
                  .append(" (").append(sub.getCredits()).append(" tín chỉ)\n");
        }

        prompt.append("\nNhiệm vụ: Dựa vào lịch sử học tập và danh sách môn có thể đăng ký, hãy đề xuất tối đa ")
              .append(MAX_RECOMMENDATIONS)
              .append(" môn học phù hợp nhất.\n");

        prompt.append("CHÚ Ý: TRẢ VỀ ĐÚNG FORMAT JSON DƯỚI ĐÂY VÀ CHỈ JSON.\n");
        prompt.append("{\n");
        prompt.append("  \"explanation\": \"Lý do tổng quan ngắn gọn.\",\n");
        prompt.append("  \"recommendedSubjects\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"subjectCode\": \"Mã môn\",\n");
        prompt.append("      \"score\": 95,\n");
        prompt.append("      \"reason\": \"Lý do chọn\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}");

        return prompt.toString();
    }
    
    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\n\\r{}]", "").trim();
    }
    private RecommendationResponse parseAndMapResponse(String aiJsonText, List<ProgramSubject> candidateSubjects) {
        try {
            String cleanJson = extractJsonFromAiResponse(aiJsonText);

            JsonNode rootResponse = objectMapper.readTree(cleanJson);
            String explanation = rootResponse.path("explanation").asText();
            JsonNode recommendedList = rootResponse.path("recommendedSubjects");

            List<RecommendationResponse.SubjectRecommendation> recommendations = new ArrayList<>();
            
            Map<String, Subject> candidateMap = candidateSubjects.stream()
                    .collect(Collectors.toMap(ps -> ps.getSubject().getCode().trim().toUpperCase(), ProgramSubject::getSubject, (s1, s2) -> s1));

            if (recommendedList.isArray()) {
                for (JsonNode recNode : recommendedList) {
                    String code = recNode.path("subjectCode").asText().trim().toUpperCase();
                    if (candidateMap.containsKey(code)) {
                        Subject actualSubject = candidateMap.get(code);
                        int rawScore = recNode.path("score").asInt(0);
                        int clampedScore = Math.min(100, Math.max(0, rawScore));
                        
                        recommendations.add(RecommendationResponse.SubjectRecommendation.builder()
                                .subjectId(actualSubject.getId())
                                .subjectCode(actualSubject.getCode())
                                .subjectName(actualSubject.getName())
                                .score(clampedScore)
                                .reason(sanitizeText(recNode.path("reason").asText()))
                                .build()
                        );
                    }
                }
            }

            recommendations.sort((a, b) -> Integer.compare(b.getScore() != null ? b.getScore() : 0, a.getScore() != null ? a.getScore() : 0));

            return RecommendationResponse.builder()
                    .explanation(explanation)
                    .recommendedSubjects(recommendations)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse JSON from AI response. Output: {}", aiJsonText, e);
            throw new RuntimeException("Lỗi khi xử lý phản hồi JSON từ AI.", e);
        }
    }

    private String extractJsonFromAiResponse(String text) {
        if (text == null) return "{}";
        Matcher matcher = Pattern.compile("\\{.*\\}", Pattern.DOTALL).matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "{}";
    }
}
