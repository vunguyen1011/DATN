package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.StudentGradeRequest;
import com.example.datn.DTO.Response.StudentGradeResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.Enrollment;
import com.example.datn.Model.StudentGrade;
import com.example.datn.Repository.EnrollmentRepository;
import com.example.datn.Repository.StudentGradeRepository;
import com.example.datn.Repository.StudentRepository;
import com.example.datn.Repository.UserRepository;
import com.example.datn.Service.Interface.IStudentGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentGradeService implements IStudentGradeService {

    private final StudentGradeRepository studentGradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional
    public StudentGradeResponse upsertGrade(StudentGradeRequest request) {
        // Tìm enrollment
        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Enrollment không tồn tại: " + request.getEnrollmentId()));

        // Tìm grade hiện có (nếu có) hoặc tạo mới
        StudentGrade grade = studentGradeRepository.findByEnrollmentId(request.getEnrollmentId())
                .orElse(StudentGrade.builder().enrollment(enrollment).build());

        // Cập nhật điểm
        grade.setMidtermScore(request.getMidtermScore());
        grade.setFinalScore(request.getFinalScore());
        grade.setTotalScore(request.getTotalScore());

        // isPassed: Admin cấu hình thủ công, hoặc tự tính nếu null (totalScore >= 5.0)
        if (request.getIsPassed() != null) {
            grade.setIsPassed(request.getIsPassed());
        } else if (request.getTotalScore() != null) {
            grade.setIsPassed(request.getTotalScore() >= 5.0);
        }
        // else giữ nguyên giá trị mặc định false

        grade = studentGradeRepository.save(grade);
        return toResponse(grade);
    }

    @Override
    public List<StudentGradeResponse> getMyTranscript() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        var student = studentRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return studentGradeRepository.findAllByStudentId(student.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Set<UUID> getPassedSubjectIds(UUID studentId) {
        return studentGradeRepository.findPassedSubjectIdsByStudentId(studentId);
    }

    // ─── Mapper nội bộ ──────────────────────────────────────────────────────────

    private StudentGradeResponse toResponse(StudentGrade grade) {
        var enrollment = grade.getEnrollment();
        var classSection = enrollment.getClassSection();
        var subject = classSection.getSubject();
        var semester = classSection.getSemester();
        var student = enrollment.getStudent();

        return StudentGradeResponse.builder()
                .gradeId(grade.getId())
                .enrollmentId(enrollment.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentName(student.getFullName())
                .subjectId(subject.getId())
                .subjectCode(subject.getCode())
                .subjectName(subject.getName())
                .credits(subject.getCredits())
                .semesterId(semester.getId())
                .semesterName(semester.getName())
                .midtermScore(grade.getMidtermScore())
                .finalScore(grade.getFinalScore())
                .totalScore(grade.getTotalScore())
                .isPassed(grade.getIsPassed())
                .build();
    }
}
