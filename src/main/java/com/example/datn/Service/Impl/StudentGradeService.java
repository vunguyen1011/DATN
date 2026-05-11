package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.FinalGradeRequest;
import com.example.datn.DTO.Request.MidtermGradeRequest;
import com.example.datn.DTO.Response.StudentGradeResponse;
import com.example.datn.ENUM.EnrollmentStatus;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentGradeService implements IStudentGradeService {

    private final StudentGradeRepository studentGradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final com.example.datn.Repository.ProgramSubjectRepository programSubjectRepository;
    private final com.example.datn.Repository.ScheduleRepository scheduleRepository;

    @Override
    @Transactional
    @CacheEvict(value = "passedSubjects", key = "#result.studentId")
    public StudentGradeResponse updateMidtermScore(UUID enrollmentId, Double midtermScore) {
        StudentGrade grade = getOrCreateGrade(enrollmentId);
        grade.setMidtermScore(midtermScore);

        recalculateFinalResult(grade);
        grade = studentGradeRepository.save(grade);

        return toResponse(grade);
    }

    @Override
    @Transactional
    @CacheEvict(value = "passedSubjects", key = "#result.studentId")
    public StudentGradeResponse updateFinalScore(UUID enrollmentId, Double finalScore) {
        StudentGrade grade = getOrCreateGrade(enrollmentId);
        grade.setFinalScore(finalScore);

        recalculateFinalResult(grade);
        grade = studentGradeRepository.save(grade);

        return toResponse(grade);
    }

    @Override
    @Transactional
    public void updateClassSectionMidtermGrades(UUID classSectionId, List<MidtermGradeRequest> requests) {
        validateLecturerAccess(classSectionId);
        for (MidtermGradeRequest req : requests) {
            updateMidtermScore(req.getEnrollmentId(), req.getMidtermScore());
        }
    }

    @Override
    @Transactional
    public void updateClassSectionFinalGrades(UUID classSectionId, List<FinalGradeRequest> requests) {
        validateLecturerAccess(classSectionId);
        for (FinalGradeRequest req : requests) {
            updateFinalScore(req.getEnrollmentId(), req.getFinalScore());
        }
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

    @Cacheable(value = "passedSubjects", key = "#studentId")
    public Set<UUID> getPassedSubjectIds(UUID studentId) {
        return studentGradeRepository.findPassedSubjectIdsByStudentId(studentId);
    }

    @Override
    public com.example.datn.DTO.Response.TranscriptTreeResponse getMyTranscriptTree() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        var student = studentRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<com.example.datn.Model.ProgramSubject> programSubjects = programSubjectRepository
                .findSubjectsByCohortAndMajor(student.getCohort().getId(), student.getMajor().getId());

        if (programSubjects.isEmpty()) {
            return com.example.datn.DTO.Response.TranscriptTreeResponse.builder()
                    .subjectGroups(List.of())
                    .build();
        }

        var program = programSubjects.get(0).getSection().getEducationProgram();
        List<StudentGrade> studentGrades = studentGradeRepository.findAllByStudentId(student.getId());

        Map<UUID, StudentGrade> gradeMapBySubjectId = studentGrades.stream()
                .collect(Collectors.toMap(
                        sg -> sg.getEnrollment().getClassSection().getSubject().getId(),
                        sg -> sg,
                        (existing, replacement) -> replacement
                ));

        Map<com.example.datn.Model.SubjectGroup, Map<com.example.datn.Model.SubjectGroupSection, List<com.example.datn.Model.ProgramSubject>>> groupedSubjects =
                programSubjects.stream().collect(Collectors.groupingBy(
                        ps -> ps.getSection().getSubjectGroup(),
                        Collectors.groupingBy(com.example.datn.Model.ProgramSubject::getSection)
                ));

        List<com.example.datn.DTO.Response.SubjectGroupTreeDTO> groupDTOs = new ArrayList<>();
        List<com.example.datn.Model.SubjectGroup> sortedGroups = new ArrayList<>(groupedSubjects.keySet());
        sortedGroups.sort(Comparator.comparing(g -> g.getIndex() != null ? g.getIndex() : 999));

        for (var group : sortedGroups) {
            var sectionsMap = groupedSubjects.get(group);
            List<com.example.datn.DTO.Response.SubjectGroupSectionTreeDTO> sectionDTOs = new ArrayList<>();

            List<com.example.datn.Model.SubjectGroupSection> sortedSections = new ArrayList<>(sectionsMap.keySet());
            sortedSections.sort(Comparator.comparing(s -> s.getIndex() != null ? s.getIndex() : 999));

            for (var section : sortedSections) {
                List<com.example.datn.Model.ProgramSubject> subjects = sectionsMap.get(section);

                List<com.example.datn.DTO.Response.SubjectGradeTreeDTO> subjectDTOs = subjects.stream().map(ps -> {
                    var subject = ps.getSubject();
                    StudentGrade grade = gradeMapBySubjectId.get(subject.getId());

                    return com.example.datn.DTO.Response.SubjectGradeTreeDTO.builder()
                            .subjectId(subject.getId())
                            .subjectCode(subject.getCode())
                            .subjectName(subject.getName())
                            .credits(subject.getCredits())
                            .midtermScore(grade != null ? grade.getMidtermScore() : null)
                            .finalScore(grade != null ? grade.getFinalScore() : null)
                            .totalScore(grade != null ? grade.getTotalScore() : null)
                            .isPassed(grade != null ? grade.getIsPassed() : null)
                            .letterGrade(grade != null ? getLetterGrade(grade.getTotalScore()) : null)
                            .build();
                }).collect(Collectors.toList());

                sectionDTOs.add(com.example.datn.DTO.Response.SubjectGroupSectionTreeDTO.builder()
                        .sectionTitle(section.getIsMandatory() ? "Bắt buộc" : "Tự chọn")
                        .subjects(subjectDTOs)
                        .build());
            }

            groupDTOs.add(com.example.datn.DTO.Response.SubjectGroupTreeDTO.builder()
                    .groupName(group.getName())
                    .sections(sectionDTOs)
                    .build());
        }

        return com.example.datn.DTO.Response.TranscriptTreeResponse.builder()
                .programId(program.getId())
                .programCode(program.getCode())
                .programName(program.getName())
                .subjectGroups(groupDTOs)
                .build();
    }

    @Override
    public List<com.example.datn.DTO.Response.ClassSectionStudentGradeResponse> getStudentsGradesByClassSection(UUID classSectionId) {
        validateLecturerAccess(classSectionId);

        List<Enrollment> enrollments = enrollmentRepository.findByClassSection_IdAndStatus(classSectionId, EnrollmentStatus.REGISTERED);

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<StudentGrade> grades = studentGradeRepository.findAllByClassSectionId(classSectionId);
        Map<UUID, StudentGrade> gradeMapByEnrollmentId = grades.stream()
                .collect(Collectors.toMap(
                        g -> g.getEnrollment().getId(),
                        g -> g
                ));

        return enrollments.stream().map(enrollment -> {
            var student = enrollment.getStudent();
            var adminClass = student.getAdminClass();
            var grade = gradeMapByEnrollmentId.get(enrollment.getId());

            return com.example.datn.DTO.Response.ClassSectionStudentGradeResponse.builder()
                    .enrollmentId(enrollment.getId())
                    .studentId(student.getId())
                    .studentCode(student.getStudentCode())
                    .studentName(student.getFullName())
                    .adminClassName(adminClass != null ? adminClass.getName() : null)
                    .midtermScore(grade != null ? grade.getMidtermScore() : null)
                    .finalScore(grade != null ? grade.getFinalScore() : null)
                    .totalScore(grade != null ? grade.getTotalScore() : null)
                    .letterGrade(grade != null ? getLetterGrade(grade.getTotalScore()) : null)
                    .isPassed(grade != null ? grade.getIsPassed() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private StudentGrade getOrCreateGrade(UUID enrollmentId) {
        return studentGradeRepository.findByEnrollmentId(enrollmentId)
                .orElseGet(() -> {
                    Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                            .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Enrollment không tồn tại: " + enrollmentId));
                    return StudentGrade.builder().enrollment(enrollment).build();
                });
    }

    private void recalculateFinalResult(StudentGrade grade) {
        if (grade.getMidtermScore() != null && grade.getFinalScore() != null) {
            double total = (grade.getMidtermScore() * 0.4) + (grade.getFinalScore() * 0.6);
            grade.setTotalScore((double) Math.round(total * 10) / 10);
            grade.setIsPassed(grade.getTotalScore() >= 5.0);
        }
    }


    private void validateLecturerAccess(UUID classSectionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Kiểm tra nếu là Admin thì cho qua luôn
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN") || role.getAuthority().equals("ADMIN"));

        if (isAdmin) {
            return;
        }

        // 2. Nếu không phải Admin, thì phải là Giảng viên đang dạy lớp đó
        String username = authentication.getName();
        boolean isAuthorized = scheduleRepository.existsByClassSection_IdAndLecturer_User_Username(classSectionId, username);

        if (!isAuthorized) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thao tác trên lớp học phần này.");
        }
    }

    private String getLetterGrade(Double totalScore) {
        if (totalScore == null) return null;
        if (totalScore >= 8.5) return "A";
        if (totalScore >= 7.0) return "B";
        if (totalScore >= 5.5) return "C";
        if (totalScore >= 4.0) return "D";
        return "F";
    }

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