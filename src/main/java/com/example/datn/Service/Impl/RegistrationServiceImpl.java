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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    @Value("${app.registration.max-credits:25}")
    private int maxCreditsPerSemester;

    // --- HELPER METHODS (Dữ liệu tĩnh nên cân nhắc @Cacheable ở bước sau) ---

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

    // --- CORE LOGIC ---

    @Override
    // KHÔNG dùng @Transactional ở đây để tối ưu Connection Pool
    public List<com.example.datn.DTO.Response.EnrollmentSimpleResponse> enroll(EnrollRequest request) {

        // 1. GIAI ĐOẠN VALIDATION (NGOÀI TRANSACTION)
        Student student = getCurrentStudent();
        PeriodCohort activePeriod = getActivePeriodCohort(student.getCohort().getId());
        UUID semesterId = activePeriod.getRegistrationPeriod().getSemester().getId();

        // Tải thông tin lớp học (Sử dụng FindById thông thường, Connection trả về ngay)
        ClassSection theorySection = classSectionRepository.findById(request.getTheoryClassId())
                .orElseThrow(() -> new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp lý thuyết"));

        validateClassBasics(theorySection, semesterId);

        // Xử lý lớp thực hành (Lab)
        ClassSection labSection = validateAndGetLabSection(request, theorySection);

        // Kiểm tra trùng môn & Tải danh sách đã đăng ký
        List<Enrollment> currentEnrollments = enrollmentRepository.findActiveEnrollmentsBySemester(
                student.getId(), semesterId, EnrollmentStatus.REGISTERED);

        validateDuplicateSubject(currentEnrollments, theorySection);

        // Kiểm tra điều kiện tiên quyết (Đã được tối ưu)
        checkPrerequisitesOptimized(student.getId(), theorySection.getSubject().getId());

        // Kiểm tra trùng lịch (Dùng Query DB nhưng gom lại một chỗ ngoài Transaction)
        validateSchedules(theorySection, labSection, currentEnrollments);

        // PRE-FETCH: Load dữ liệu Lazy trước khi vào Transaction để tránh LazyInitializationException
        theorySection.getSubject().getName();
        if (labSection != null) labSection.getSubject().getName();

        // Chuẩn bị các đối tượng Enrollment (Trạng thái transient - chưa save)
        Enrollment theoryEnrollment = prepareEnrollment(student, theorySection);
        final Enrollment finalLabEnrollment = (labSection != null) ? prepareEnrollment(student, labSection) : null;
        final ClassSection finalLabSection = labSection;

        // 2. GIAI ĐOẠN PERSISTENCE (TRONG TRANSACTION)
        // Connection chỉ bị chiếm dụng từ đây
        return transactionTemplate.execute(status -> {
            try {
                // Atomic Update: Increment kèm check sĩ số tại tầng DB
                if (classSectionRepository.tryIncrementEnrolledCount(theorySection.getId()) == 0) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp lý thuyết đã đủ sĩ số");
                }

                if (finalLabSection != null) {
                    if (classSectionRepository.tryIncrementEnrolledCount(finalLabSection.getId()) == 0) {
                        throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành đã đủ sĩ số");
                    }
                }

                // Lưu bản ghi
                List<com.example.datn.DTO.Response.EnrollmentSimpleResponse> responses = new ArrayList<>();
                responses.add(enrollmentMapper.toSimpleResponse(enrollmentRepository.save(theoryEnrollment)));

                if (finalLabEnrollment != null) {
                    responses.add(enrollmentMapper.toSimpleResponse(enrollmentRepository.save(finalLabEnrollment)));
                }

                return responses;
            } catch (Exception e) {
                status.setRollbackOnly(); // Đảm bảo Rollback nếu có lỗi xảy ra
                throw e;
            }
        });
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

        // Hủy các lớp con (Lab) liên quan
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

    // --- CÁC HÀM VALIDATION TÁCH RỜI (CLEAN CODE) ---

    private void validateClassBasics(ClassSection theory, UUID currentSemesterId) {
        if (!theory.getSemester().getId().equals(currentSemesterId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp học phần không thuộc học kỳ hiện tại");
        }
        if (theory.getParentSection() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã ID phải là của lớp lý thuyết gốc");
        }
    }

    private ClassSection validateAndGetLabSection(EnrollRequest request, ClassSection theory) {
        boolean subjectHasLabs = classSectionRepository.existsByParentSectionId(theory.getId());
        if (request.getLabClassId() != null) {
            ClassSection lab = classSectionRepository.findById(request.getLabClassId())
                    .orElseThrow(() -> new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp thực hành"));
            if (lab.getParentSection() == null || !lab.getParentSection().getId().equals(theory.getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành không thuộc về lớp lý thuyết");
            }
            return lab;
        } else if (subjectHasLabs) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Môn học này yêu cầu chọn 1 ca thực hành");
        }
        return null;
    }

    private void validateDuplicateSubject(List<Enrollment> current, ClassSection target) {
        for (Enrollment en : current) {
            if (en.getClassSection().getSubject().getId().equals(target.getSubject().getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đã đăng ký môn học này rồi");
            }
        }
    }

    private void validateSchedules(ClassSection theory, ClassSection lab, List<Enrollment> current) {
        List<UUID> enrolledIds = current.stream().map(en -> en.getClassSection().getId()).collect(Collectors.toList());

        if (lab != null) {
            if (scheduleRepository.countOverlappingSchedules(theory.getId(), Collections.singletonList(lab.getId())) > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Trùng lịch giữa lý thuyết và thực hành");
            }
        }

        if (!enrolledIds.isEmpty()) {
            if (scheduleRepository.countOverlappingSchedules(theory.getId(), enrolledIds) > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp lý thuyết trùng lịch với các môn đã đăng ký");
            }
            if (lab != null && scheduleRepository.countOverlappingSchedules(lab.getId(), enrolledIds) > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành trùng lịch với các môn đã đăng ký");
            }
        }
    }

    private Enrollment prepareEnrollment(Student student, ClassSection section) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassSectionId(student.getId(), section.getId())
                .orElse(Enrollment.builder().student(student).classSection(section).build());
        enrollment.setStatus(EnrollmentStatus.REGISTERED);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        return enrollment;
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