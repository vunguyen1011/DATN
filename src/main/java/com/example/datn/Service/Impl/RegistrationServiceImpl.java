package com.example.datn.Service.Impl;

import com.example.datn.Config.EnrollmentCacheManager;
import com.example.datn.DTO.Request.EnrollmentSaveRequest;
import com.example.datn.DTO.Request.EnrollRequest;
import com.example.datn.DTO.Response.EnrollmentResponse;
import com.example.datn.DTO.Response.EnrollmentSimpleResponse;
import com.example.datn.DTO.Response.RegistrationStatusResponse;
import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.EnrollmentMapper;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IPeriodCohortService;
import com.example.datn.Service.Interface.IRedisService;
import com.example.datn.Service.Interface.IRegistrationService;
import com.example.datn.Service.Interface.IStudentGradeService;
import com.example.datn.Service.Interface.ISubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements IRegistrationService {

    private final StudentRepository studentRepository;
    private final PeriodCohortRepository periodCohortRepository;
    private final ClassSectionRepository classSectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final IStudentGradeService studentGradeService;
    private final ISubjectService subjectService;
    private final SemesterRepository semesterRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final IRedisService redisService;
    private final AsyncEnrollmentPersister asyncEnrollmentPersister;
    private final EnrollmentCacheManager enrollmentCacheManager;
    private final IPeriodCohortService periodCohortService;

    @Value("${app.registration.max-credits:25}")
    private int maxCreditsPerSemester;

    private Student getCurrentStudent() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof com.example.datn.Security.MyUserDetail userDetails) {
            if (userDetails.getStudentId() != null) {
                Student lightweightStudent = new Student();
                lightweightStudent.setId(userDetails.getStudentId());
                if (userDetails.getCohortId() != null) {
                    Cohort cohort = new Cohort();
                    cohort.setId(userDetails.getCohortId());
                    lightweightStudent.setCohort(cohort);
                }
                return lightweightStudent;
            }
        }
        String username = authentication.getName();
        return studentRepository.findByUser_Username(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private PeriodCohort getActivePeriodCohort(UUID cohortId) {
        return periodCohortService.getOngoingCohortPeriod(cohortId, LocalDateTime.now());
    }

    @Override
    public List<EnrollmentSimpleResponse> enroll(EnrollRequest request) {

        Student student = getCurrentStudent();
        PeriodCohort activePeriod = getActivePeriodCohort(student.getCohort().getId());
        UUID semesterId = activePeriod.getRegistrationPeriod().getSemester().getId();

        ClassSection theorySection = classSectionRepository.findByIdWithDetails(request.getTheoryClassId())
                .orElseThrow(() -> new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp lý thuyết"));

        validateClassBasics(theorySection, semesterId);
        ClassSection labSection = validateAndGetLabSection(request, theorySection);

        List<EnrollmentResponse> currentEnrollments = getMyTimetable();

        validateDuplicateSubject(currentEnrollments, theorySection);
        checkPrerequisitesOptimized(student.getId(), theorySection.getSubject().getId());

        validateSchedulesWithBatchQuery(theorySection, labSection, currentEnrollments);

        String theorySubjectName = theorySection.getSubject().getName();
        String labSubjectName = (labSection != null) ? labSection.getSubject().getName() : null;

        // ── GIAI ĐOẠN 2: REDIS LUA SCRIPT ────────
        int theoryResult = redisService.tryAcquireSlot(theorySection.getId(), student.getId());
        if (theoryResult == 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đã đăng ký lớp học phần này rồi");
        }
        if (theoryResult == -1) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp lý thuyết đã hết chỗ");
        }

        if (labSection != null) {
            int labResult = redisService.tryAcquireSlot(labSection.getId(), student.getId());
            if (labResult != 1) {
                redisService.releaseSlot(theorySection.getId(), student.getId());
                String msg = (labResult == 0) ? "Bạn đã đăng ký lớp thực hành này rồi" : "Lớp thực hành đã hết chỗ";
                throw new AppException(ErrorCode.INVALID_REQUEST, msg);
            }
        }

        // ── GIAI ĐOẠN 3: CHUẨN BỊ OBJECT & TRẢ KẾT QUẢ NGAY ───────────
        Enrollment theoryEnrollment = prepareEnrollment(student, theorySection);
        List<Enrollment> toSave = new ArrayList<>();
        toSave.add(theoryEnrollment);

        List<EnrollmentSimpleResponse> responses = new ArrayList<>();
        responses.add(buildSimpleResponse(theoryEnrollment, theorySubjectName));

        if (labSection != null) {
            Enrollment labEnrollment = prepareEnrollment(student, labSection);
            toSave.add(labEnrollment);
            responses.add(buildSimpleResponse(labEnrollment, labSubjectName));
        }

        // ── GIAI ĐOẠN 4: ASYNC SAVE ─────────────
        // Convert sang DTO TRONG HTTP thread (session còn sống) để tránh
        // StaleObjectStateException
        List<EnrollmentSaveRequest> saveRequests = toSave.stream()
                .map(EnrollmentSaveRequest::from)
                .collect(Collectors.toList());
        asyncEnrollmentPersister.saveToDatabaseAsync(saveRequests, true);
        return responses;
    }

    @Override
    // ĐÃ BỎ @Transactional Ở ĐÂY ĐỂ ĐẨY XUỐNG ASYNC
    public void cancelEnrollment(UUID theoryClassSectionId) {
        Student student = getCurrentStudent();
        getActivePeriodCohort(student.getCohort().getId());

        Enrollment theoryEnrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(student.getId(), theoryClassSectionId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND, "Không tìm thấy dữ liệu đăng ký"));

        if (theoryEnrollment.getStatus() != EnrollmentStatus.REGISTERED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp này chưa được đăng ký hoặc đã bị hủy");
        }

        ClassSection theorySection = theoryEnrollment.getClassSection();
        if (theorySection.getParentSection() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Vui lòng truyền ID của lớp lý thuyết để hủy toàn bộ môn học");
        }

        List<Enrollment> enrollmentsToCancel = new ArrayList<>();

        // Hủy Lý thuyết và trả slot Redis
        theoryEnrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentsToCancel.add(theoryEnrollment);
        redisService.releaseSlot(theoryClassSectionId, student.getId());

        // Hủy Thực hành đi kèm (nếu có) và trả slot Redis
        List<Enrollment> currentEnrollments = enrollmentRepository.findActiveEnrollmentsBySemester(
                student.getId(), theorySection.getSemester().getId(), EnrollmentStatus.REGISTERED);

        for (Enrollment en : currentEnrollments) {
            ClassSection sec = en.getClassSection();
            if (sec.getParentSection() != null && sec.getParentSection().getId().equals(theoryClassSectionId)) {
                en.setStatus(EnrollmentStatus.CANCELLED);
                enrollmentsToCancel.add(en);
                redisService.releaseSlot(sec.getId(), student.getId());
                break;
            }
        }

        // Đẩy việc lưu DB trạng thái CANCEL xuống worker chạy ngầm
        // Convert sang DTO TRONG HTTP thread (session còn sống) để tránh
        // StaleObjectStateException
        List<EnrollmentSaveRequest> cancelRequests = enrollmentsToCancel.stream()
                .map(EnrollmentSaveRequest::from)
                .collect(Collectors.toList());
        asyncEnrollmentPersister.saveToDatabaseAsync(cancelRequests, false);
    }

    // ─────────────────────────────────────────────────────
    // QUERIES
    // ─────────────────────────────────────────────────────

    @Override
    public Page<EnrollmentResponse> getAllEnrollmentInClassSection(UUID classSectionId, Pageable pageable) {
        return enrollmentRepository
                .findByClassSection_IdAndStatus(classSectionId, EnrollmentStatus.REGISTERED, pageable)
                .map(enrollmentMapper::toResponse);
    }

    public UUID getCurrentStudentCohortId() {
        return getCurrentStudent().getCohort().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationStatusResponse getRegistrationStatus() {
        UUID cohortId = getCurrentStudentCohortId();
        Optional<PeriodCohort> activePeriodOpt = periodCohortRepository
                .findOngoingCohortPeriod(cohortId, LocalDateTime.now());

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
    @Cacheable(value = "enrolledSections", key = "T(com.example.datn.Util.SecurityUtils).getCurrentStudentId()", condition = "T(com.example.datn.Util.SecurityUtils).getCurrentStudentId() != null", cacheManager = "redisCacheManager")
    public List<EnrollmentResponse> getMyTimetable() {
        Student student = getCurrentStudent();

        // 1. Lấy Học kỳ hiện tại thay vì phụ thuộc vào Đợt đăng ký
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(
                        () -> new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND, "Không tìm thấy học kỳ hiện tại"));

        // 2. Trả về danh sách môn học sinh viên đã đăng ký trong học kỳ đó
        return enrollmentRepository.findActiveEnrollmentsBySemester(
                student.getId(),
                currentSemester.getId(),
                EnrollmentStatus.REGISTERED)
                .stream()
                .map(enrollmentMapper::toResponse)
                .collect(Collectors.toList());
    }

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
            ClassSection lab = classSectionRepository.findByIdWithDetails(request.getLabClassId())
                    .orElseThrow(
                            () -> new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp thực hành"));
            if (lab.getParentSection() == null || !lab.getParentSection().getId().equals(theory.getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành không thuộc về lớp lý thuyết");
            }
            return lab;
        } else if (subjectHasLabs) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Môn học này yêu cầu chọn 1 ca thực hành");
        }
        return null;
    }

    private void validateDuplicateSubject(List<EnrollmentResponse> current, ClassSection target) {
        for (EnrollmentResponse en : current) {
            if (en.getClassSection().getSubjectCode().equals(target.getSubject().getCode())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đã đăng ký môn học này rồi");
            }
        }
    }

    // Kiểm tra trùng lịch bằng 1 batch query duy nhất rồi so sánh trong RAM.
    // Tránh các vấn đề của approach cũ:
    // - scheduleRepository.countOverlappingSchedules() gọi nhiều lần = nhiều query
    // - theory.getSchedules() = LazyInitializationException (ClassSection không có
    // @OneToMany schedules)
    private void validateSchedulesWithBatchQuery(ClassSection theory, ClassSection lab, List<EnrollmentResponse> current) {
        // Tạp hợp tất cả ID cần lấy lịch trong 1 query batch
        List<UUID> sectionIdsToFetch = new ArrayList<>();
        sectionIdsToFetch.add(theory.getId());
        if (lab != null)
            sectionIdsToFetch.add(lab.getId());
        current.forEach(en -> sectionIdsToFetch.add(en.getClassSection().getId()));

        // Tái sử dụng @Cacheable("classSectionSchedules") bằng vòng lặp (Zero DB Hit) thay vì Batch Query
        List<Schedule> allSchedules = new ArrayList<>();
        for (UUID sid : sectionIdsToFetch) {
            allSchedules.addAll(scheduleRepository.findByClassSection_Id(sid));
        }

        // Phân nhóm theo classSectionId trong RAM (không gọi DB nữa)
        Map<UUID, List<Schedule>> scheduleMap = allSchedules.stream()
                .collect(Collectors.groupingBy(s -> s.getClassSection().getId()));

        List<Schedule> theorySchedules = scheduleMap.getOrDefault(theory.getId(), List.of());
        List<Schedule> labSchedules = lab != null ? scheduleMap.getOrDefault(lab.getId(), List.of()) : List.of();
        List<Schedule> newSchedules = new ArrayList<>(theorySchedules);
        newSchedules.addAll(labSchedules);

        List<Schedule> currentSchedules = current.stream()
                .flatMap(en -> scheduleMap.getOrDefault(en.getClassSection().getId(), List.of()).stream())
                .collect(Collectors.toList());

        // So sánh in-memory — không chạm DB
        if (lab != null && hasOverlap(theorySchedules, labSchedules)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Trùng lịch giữa lý thuyết và thực hành");
        }
        if (hasOverlap(newSchedules, currentSchedules)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lịch học bị trùng với các môn đã đăng ký");
        }
    }

    // ĐÃ TỐI ƯU: THUẬT TOÁN BITMASK ĐỂ SO SÁNH LỊCH HỌC TRONG 1 NANOGIÂY (O(1))
    private int[] buildScheduleMask(List<Schedule> schedules) {
        int[] mask = new int[9]; // index 2 -> 8 tương ứng với Thứ 2 -> Chủ Nhật
        for (Schedule s : schedules) {
            if (s.getDayOfWeek() == null || s.getStartPeriod() == null || s.getEndPeriod() == null) continue;
            int day = s.getDayOfWeek();
            int start = s.getStartPeriod();
            int end = s.getEndPeriod();
            // Tạo chuỗi bit tương ứng với các tiết học. Ví dụ tiết 1-3: bit 1, 2, 3 sẽ bật lên 1.
            int periodMask = ((1 << (end - start + 1)) - 1) << start;
            mask[day] |= periodMask;
        }
        return mask;
    }

    private boolean hasOverlap(List<Schedule> list1, List<Schedule> list2) {
        int[] mask1 = buildScheduleMask(list1);
        int[] mask2 = buildScheduleMask(list2);
        
        // Phép toán Bitwise AND (&) cực kỳ nhẹ cho CPU so với so sánh vòng lặp nồng nhau
        for (int i = 2; i <= 8; i++) {
            if ((mask1[i] & mask2[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    // ĐÃ SỬA: TỰ CẤP PHÁT UUID ĐỂ TRÁNH LỖI RESPONSE NULL ID
    private Enrollment prepareEnrollment(Student student, ClassSection section) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassSectionId(student.getId(), section.getId())
                .orElseGet(() -> {
                    Enrollment newEn = Enrollment.builder()
                            .student(studentRepository.getReferenceById(student.getId()))
                            .classSection(section)
                            .build();
                    newEn.setId(UUID.randomUUID()); // Cấp phát UUID ngay tại đây
                    return newEn;
                });
        enrollment.setStatus(EnrollmentStatus.REGISTERED);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        return enrollment;
    }

    private EnrollmentSimpleResponse buildSimpleResponse(Enrollment enrollment, String subjectName) {
        return EnrollmentSimpleResponse.builder()
                .classSectionId(enrollment.getClassSection().getId())
                .subjectName(subjectName)
                .status(enrollment.getStatus())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .build();
    }

    private void checkPrerequisitesOptimized(UUID studentId, UUID subjectId) {
        List<com.example.datn.DTO.Response.SubjectResponse> prerequisites = subjectService.getPrerequisites(subjectId);
        if (prerequisites == null || prerequisites.isEmpty())
            return;

        Set<UUID> passedSubjectIds = studentGradeService.getPassedSubjectIds(studentId);

        for (com.example.datn.DTO.Response.SubjectResponse prereq : prerequisites) {
            if (!passedSubjectIds.contains(prereq.getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Chưa đạt môn tiên quyết: " + prereq.getName());
            }
        }
    }

    @Override
    public void clearRedisDataAfterRegistration() {
        redisService.clearRegistrationData();
    }
}