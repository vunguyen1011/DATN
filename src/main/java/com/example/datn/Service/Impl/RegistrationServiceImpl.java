package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EnrollRequest;
import com.example.datn.DTO.Response.EnrollmentResponse;
import com.example.datn.DTO.Response.RegistrationStatusResponse;
import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.EnrollmentMapper;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements IRegistrationService {

    private final StudentRepository studentRepository;
    private final PeriodCohortRepository periodCohortRepository;
    private final ClassSectionRepository classSectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final EnrollmentMapper enrollmentMapper;

    @Value("${app.registration.max-credits:25}")
    private int maxCreditsPerSemester;

    private Student getCurrentStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return studentRepository.findByUser_Username(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private PeriodCohort getActivePeriodCohort(UUID cohortId) {
        LocalDateTime now = LocalDateTime.now();
        return periodCohortRepository.findOngoingCohortPeriod(cohortId, now)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Ngoài thời gian đăng ký tín chỉ"));
    }

    @Override
    @Transactional
    public List<com.example.datn.DTO.Response.EnrollmentSimpleResponse> enroll(EnrollRequest request) {
        Student student = getCurrentStudent();

        PeriodCohort activePeriod = getActivePeriodCohort(student.getCohort().getId());
        UUID semesterId = activePeriod.getRegistrationPeriod().getSemester().getId();

        ClassSection theorySection = classSectionRepository.findById(request.getTheoryClassId())
                .orElseThrow(() -> new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp lý thuyết"));

        if (!theorySection.getSemester().getId().equals(semesterId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp học phần không thuộc học kỳ hiện tại");
        }

        if (theorySection.getParentSection() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "theoryClassId phải là mã của lớp lý thuyết gốc");
        }

        boolean subjectHasLabs = classSectionRepository.existsByParentSectionId(theorySection.getId());

        ClassSection labSection = null;
        if (request.getLabClassId() != null) {
            labSection = classSectionRepository.findById(request.getLabClassId())
                    .orElseThrow(() -> new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp thực hành"));

            if (labSection.getParentSection() == null || !labSection.getParentSection().getId().equals(theorySection.getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành không thuộc về lớp lý thuyết đã chọn");
            }
        } else if (subjectHasLabs) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Môn học này có lớp thực hành, vui lòng chọn 1 ca thực hành");
        }

        List<Enrollment> currentEnrollments = enrollmentRepository.findActiveEnrollmentsBySemester(
                student.getId(), semesterId, EnrollmentStatus.REGISTERED);

        for (Enrollment en : currentEnrollments) {
            ClassSection enrolledSec = en.getClassSection();
            if (enrolledSec.getSubject().getId().equals(theorySection.getSubject().getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đã đăng ký một lớp của môn học này rồi");
            }
        }

        if (theorySection.getEnrolledCount() >= theorySection.getCapacity()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp lý thuyết đã đủ sĩ số");
        }
        if (labSection != null && labSection.getEnrolledCount() >= labSection.getCapacity()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành đã đủ sĩ số");
        }

        checkPrerequisitesOptimized(student.getId(), theorySection.getSubject().getId());

        List<UUID> enrolledSectionIds = currentEnrollments.stream()
                .map(en -> en.getClassSection().getId())
                .collect(Collectors.toList());

        if (labSection != null) {
            int internalConflict = scheduleRepository.countOverlappingSchedules(theorySection.getId(), Collections.singletonList(labSection.getId()));
            if (internalConflict > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lịch lớp lý thuyết và thực hành đang chọn bị trùng nhau");
            }
        }

        if (!enrolledSectionIds.isEmpty()) {
            int conflictCount = scheduleRepository.countOverlappingSchedules(theorySection.getId(), enrolledSectionIds);
            if (conflictCount > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp lý thuyết trùng lịch với lớp đã đăng ký");
            }
            if (labSection != null) {
                int labConflictCount = scheduleRepository.countOverlappingSchedules(labSection.getId(), enrolledSectionIds);
                if (labConflictCount > 0) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành trùng lịch với lớp đã đăng ký");
                }
            }
        }



        // PRE-FETCH LAZY DATA BEFORE ACQUIRING THE LOCK
        theorySection.getSubject().getName();
        if (labSection != null) {
            labSection.getSubject().getName();
        }

        Enrollment theoryEnrollment = enrollmentRepository.findByStudentIdAndClassSectionId(student.getId(), theorySection.getId())
                .orElse(Enrollment.builder()
                        .student(student)
                        .classSection(theorySection)
                        .build());
        theoryEnrollment.setStatus(EnrollmentStatus.REGISTERED);
        theoryEnrollment.setEnrollmentDate(LocalDateTime.now());

        Enrollment labEnrollment = null;
        if (labSection != null) {
            labEnrollment = enrollmentRepository.findByStudentIdAndClassSectionId(student.getId(), labSection.getId())
                    .orElse(Enrollment.builder()
                            .student(student)
                            .classSection(labSection)
                            .build());
            labEnrollment.setStatus(EnrollmentStatus.REGISTERED);
            labEnrollment.setEnrollmentDate(LocalDateTime.now());
        }

        // ---- LOCK ACQUIRED HERE ----
        int updatedTheory = classSectionRepository.tryIncrementEnrolledCount(theorySection.getId());
        if (updatedTheory == 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp lý thuyết đã đủ sĩ số");
        }

        if (labSection != null) {
            int updatedLab = classSectionRepository.tryIncrementEnrolledCount(labSection.getId());
            if (updatedLab == 0) {
                // Phải rollback lý thuyết nếu thực hành hết chỗ (Spring tự handle via @Transactional)
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành đã đủ sĩ số");
            }
        }

        List<com.example.datn.DTO.Response.EnrollmentSimpleResponse> responses = new ArrayList<>();
        
        responses.add(enrollmentMapper.toSimpleResponse(enrollmentRepository.save(theoryEnrollment)));

        if (labEnrollment != null) {
            responses.add(enrollmentMapper.toSimpleResponse(enrollmentRepository.save(labEnrollment)));
        }

        return responses;
    }

    @Override
    @Transactional
    public void cancelEnrollment(UUID theoryClassSectionId) {
        Student student = getCurrentStudent();
        getActivePeriodCohort(student.getCohort().getId());

        Enrollment theoryEnrollment = enrollmentRepository.findByStudentIdAndClassSectionId(student.getId(), theoryClassSectionId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND, "Không tìm thấy dữ liệu đăng ký"));

        if (theoryEnrollment.getStatus() != EnrollmentStatus.REGISTERED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp này chưa được đăng ký hoặc đã bị hủy");
        }

        ClassSection theorySection = theoryEnrollment.getClassSection();
        if (theorySection.getParentSection() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng truyền ID của lớp lý thuyết để hủy toàn bộ môn học");
        }

        theoryEnrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(theoryEnrollment);
        classSectionRepository.tryDecrementEnrolledCount(theoryClassSectionId);

        List<Enrollment> currentEnrollments = enrollmentRepository.findActiveEnrollmentsBySemester(
                student.getId(), theorySection.getSemester().getId(), EnrollmentStatus.REGISTERED);

        for (Enrollment en : currentEnrollments) {
            ClassSection sec = en.getClassSection();
            if (sec.getParentSection() != null && sec.getParentSection().getId().equals(theoryClassSectionId)) {
                en.setStatus(EnrollmentStatus.CANCELLED);
                enrollmentRepository.save(en);
                classSectionRepository.tryDecrementEnrolledCount(sec.getId());
                break;
            }
        }
    }

    private void checkPrerequisitesOptimized(UUID studentId, UUID subjectId) {
        List<Prerequisite> prerequisites = prerequisiteRepository.findBySubjectId(subjectId);
        if (prerequisites == null || prerequisites.isEmpty()) return;

        Set<UUID> passedSubjectIds = studentGradeRepository.findPassedSubjectIdsByStudentId(studentId);

        for (Prerequisite prereq : prerequisites) {
            if (!passedSubjectIds.contains(prereq.getPrerequisiteSubject().getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Chưa đạt môn tiên quyết: " + prereq.getPrerequisiteSubject().getName());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationStatusResponse getRegistrationStatus() {
        Student student = getCurrentStudent();
        Optional<PeriodCohort> activePeriodOpt = periodCohortRepository.findOngoingCohortPeriod(student.getCohort().getId(), LocalDateTime.now());

        if (activePeriodOpt.isEmpty()) {
            return RegistrationStatusResponse.builder().isEligible(false).message("Ngoài đợt đăng ký").build();
        }
        RegistrationPeriod rp = activePeriodOpt.get().getRegistrationPeriod();
        return RegistrationStatusResponse.builder()
                .isEligible(true)
                .periodName(rp.getName())
                .startTime(activePeriodOpt.get().getStartTime())
                .endTime(activePeriodOpt.get().getEndTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyTimetable() {
        Student student = getCurrentStudent();
        Optional<PeriodCohort> activePeriodOpt = periodCohortRepository.findOngoingCohortPeriod(student.getCohort().getId(), LocalDateTime.now());
        if (activePeriodOpt.isEmpty()) return List.of();

        return enrollmentRepository.findActiveEnrollmentsBySemester(student.getId(), activePeriodOpt.get().getRegistrationPeriod().getSemester().getId(), EnrollmentStatus.REGISTERED)
                .stream().map(enrollmentMapper::toResponse).collect(Collectors.toList());
    }
}