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
    public EnrollmentResponse enroll(EnrollRequest request) {
        Student student = getCurrentStudent();

        PeriodCohort activePeriod = getActivePeriodCohort(student.getCohort().getId());
        UUID semesterId = activePeriod.getRegistrationPeriod().getSemester().getId();

        ClassSection targetSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND));

        if (!targetSection.getSemester().getId().equals(semesterId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp học phần không thuộc học kỳ hiện tại");
        }

        // 1. Lấy toàn bộ môn học đã đăng ký trong kỳ để kiểm tra tập trung
        List<Enrollment> currentEnrollments = enrollmentRepository.findActiveEnrollmentsBySemester(
                student.getId(), semesterId, EnrollmentStatus.REGISTERED);

        // 2. LOGIC KIỂM TRA MÔN HỌC & QUAN HỆ LÝ THUYẾT - THỰC HÀNH
        boolean hasParentOfThisSubject = false;
        boolean hasChildOfThisSubject = false;
        UUID enrolledParentSectionId = null;

        for (Enrollment en : currentEnrollments) {
            ClassSection enrolledSec = en.getClassSection();
            if (enrolledSec.getSubject().getId().equals(targetSection.getSubject().getId())) {
                if (enrolledSec.getParentSection() == null) {
                    hasParentOfThisSubject = true;
                    enrolledParentSectionId = enrolledSec.getId();
                } else {
                    hasChildOfThisSubject = true;
                }
            }
        }

        if (targetSection.getParentSection() == null) {
            // Trường hợp 1: Sinh viên đang cố đăng ký lớp LÝ THUYẾT
            if (hasParentOfThisSubject) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đã đăng ký một lớp Lý thuyết của môn học này rồi");
            }
        } else {
            // Trường hợp 2: Sinh viên đang cố đăng ký lớp THỰC HÀNH
            if (hasChildOfThisSubject) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đã đăng ký một lớp Thực hành của môn học này rồi");
            }
            if (!hasParentOfThisSubject || !targetSection.getParentSection().getId().equals(enrolledParentSectionId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn phải đăng ký lớp Lý thuyết gốc ("
                        + targetSection.getParentSection().getSectionCode() + ") trước khi đăng ký lớp Thực hành này");
            }
        }

        // 3. KIỂM TRA SĨ SỐ (Pre-check)
        if (targetSection.getEnrolledCount() >= targetSection.getCapacity()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp học phần đã đủ sĩ số");
        }

        // 4. KIỂM TRA MÔN TIÊN QUYẾT
        checkPrerequisitesOptimized(student.getId(), targetSection.getSubject().getId());

        // 5. KIỂM TRA TRÙNG LỊCH HỌC
        if (!currentEnrollments.isEmpty()) {
            List<UUID> enrolledSectionIds = currentEnrollments.stream()
                    .map(en -> en.getClassSection().getId())
                    .collect(Collectors.toList());

            int conflictCount = scheduleRepository.countOverlappingSchedules(targetSection.getId(), enrolledSectionIds);
            if (conflictCount > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Trùng lịch với lớp đã đăng ký");
            }
        }

        // 6. FIX LỖI LAZY INITIALIZATION EXCEPTION (Mồi nhử Proxy)
        if (targetSection.getSubject() != null) {
            targetSection.getSubject().getName();
        }

        // 7. CẬP NHẬT SĨ SỐ (Atomic Update)
        int updatedRows = classSectionRepository.tryIncrementEnrolledCount(targetSection.getId());
        if (updatedRows == 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp học phần đã đủ sĩ số");
        }

        // 8. TẠO HOẶC CẬP NHẬT ĐƠN ĐĂNG KÝ (Upsert)
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassSectionId(student.getId(), targetSection.getId())
                .orElse(Enrollment.builder()
                        .student(student)
                        .classSection(targetSection)
                        .build());

        enrollment.setStatus(EnrollmentStatus.REGISTERED);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);

        return enrollmentMapper.toResponse(enrollment);
    }

    @Override
    @Transactional
    public void cancelEnrollment(UUID classSectionId) {
        Student student = getCurrentStudent();
        getActivePeriodCohort(student.getCohort().getId());

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassSectionId(student.getId(), classSectionId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND, "Không tìm thấy dữ liệu đăng ký"));

        if (enrollment.getStatus() != EnrollmentStatus.REGISTERED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp này chưa được đăng ký hoặc đã bị hủy");
        }

        // Bổ sung: Ràng buộc khi hủy lớp Lý thuyết thì phải bắt hủy Thực hành trước
        ClassSection sectionToCancel = enrollment.getClassSection();
        if (sectionToCancel.getParentSection() == null) {
            boolean hasChildEnrolled = enrollmentRepository.findActiveEnrollmentsBySemester(
                            student.getId(), sectionToCancel.getSemester().getId(), EnrollmentStatus.REGISTERED)
                    .stream()
                    .anyMatch(e -> e.getClassSection().getParentSection() != null
                            && e.getClassSection().getParentSection().getId().equals(sectionToCancel.getId()));

            if (hasChildEnrolled) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Phải hủy lớp Thực hành con trước khi hủy lớp Lý thuyết");
            }
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(enrollment);

        classSectionRepository.tryDecrementEnrolledCount(classSectionId);
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