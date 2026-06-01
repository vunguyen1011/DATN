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
    private final IPeriodCohortService periodCohortService;
    private final RabbitMQProducer rabbitMQProducer;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;



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
        
        // 1. LẤY THÔNG TIN HỌC KỲ TỪ DB (Thông qua Cache nội bộ của Spring)
        UUID cohortId = student.getCohort() != null ? student.getCohort().getId() : null;
        log.info("Bắt đầu đăng ký cho Student: {}, Cohort: {}", student.getId(), cohortId);
        
        PeriodCohort activePeriod = getActivePeriodCohort(cohortId);
        UUID semesterId = activePeriod.getRegistrationPeriod().getSemester().getId();

        // 2. LẤY METADATA LỚP TỪ REDIS (0 DB Query)
        com.example.datn.DTO.Response.ClassSectionCacheDTO theorySection = getFromRedisCache("class_metadata:" + request.getTheoryClassId());
        if (theorySection == null) {
            throw new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp lý thuyết");
        }

        if (!theorySection.getSemesterId().equals(semesterId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp học phần không thuộc học kỳ hiện hành");
        }

        com.example.datn.DTO.Response.ClassSectionCacheDTO labSection = null;
        if (request.getLabClassId() != null) {
            labSection = getFromRedisCache("class_metadata:" + request.getLabClassId());
            if (labSection == null) {
                throw new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp thực hành");
            }
            if (!theorySection.getId().equals(labSection.getParentSectionId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành không thuộc về lớp lý thuyết");
            }
        } else if (theorySection.isHasLab()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Môn học này yêu cầu chọn 1 ca thực hành");
        }

        // O(1) in-memory check prerequisites via Redis
        checkPrerequisitesOptimized(student.getId(), theorySection.getSubjectId());

        String theorySubjectName = theorySection.getSubjectName();
        String labSubjectName = (labSection != null) ? labSection.getSubjectName() : null;

        // Fetch pre-computed Class Masks from Redis
        String theoryMaskStr = redisTemplate.opsForValue().get("class_mask:" + theorySection.getId());
        String labMaskStr = labSection != null ? redisTemplate.opsForValue().get("class_mask:" + labSection.getId()) : null;
        UUID labId = labSection != null ? labSection.getId() : null;

        // ── GIAI ĐOẠN 2: REDIS LUA SCRIPT (ATOMIC VALIDATION & ACQUIRE) ────────
        java.util.UUID subjectId = theorySection.getSubjectId();
        int result = redisService.tryAcquireSlot(theorySection.getId(), labId, student.getId(), subjectId, theoryMaskStr, labMaskStr);
        
        if (result == 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đã đăng ký lớp học phần này rồi");
        }
        if (result == -1) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp lý thuyết đã hết chỗ");
        }
        if (result == -2) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đã đăng ký một lớp khác của môn học này rồi");
        }
        if (result == -3) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lịch học bị trùng với các môn đã đăng ký");
        }
        if (result == -4) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành đã hết chỗ");
        }

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

        List<EnrollmentSaveRequest> saveRequests = toSave.stream()
                .map(EnrollmentSaveRequest::from)
                .collect(Collectors.toList());
        rabbitMQProducer.sendMessage(saveRequests);
        return responses;
    }

    @Override
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
        redisService.releaseSlot(theoryClassSectionId, student.getId(), theorySection.getSubject().getId());

        // Hủy Thực hành đi kèm (nếu có) và trả slot Redis
        List<Enrollment> currentEnrollments = enrollmentRepository.findActiveEnrollmentsBySemester(
                student.getId(), theorySection.getSemester().getId(), EnrollmentStatus.REGISTERED);

        for (Enrollment en : currentEnrollments) {
            ClassSection sec = en.getClassSection();
            if (sec.getParentSection() != null && sec.getParentSection().getId().equals(theoryClassSectionId)) {
                en.setStatus(EnrollmentStatus.CANCELLED);
                enrollmentsToCancel.add(en);
                redisService.releaseSlot(sec.getId(), student.getId(), null);
                break;
            }
        }
        List<EnrollmentSaveRequest> cancelRequests = enrollmentsToCancel.stream()
                .map(EnrollmentSaveRequest::from)
                .collect(Collectors.toList());
        rabbitMQProducer.sendMessage(cancelRequests);
    }


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

    private List<EnrollmentResponse> getMyTimetableInternal(Student student) {
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

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyTimetable() {
        return getMyTimetableInternal(getCurrentStudent());
    }

    private com.example.datn.DTO.Response.ClassSectionCacheDTO getFromRedisCache(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, com.example.datn.DTO.Response.ClassSectionCacheDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void validateClassBasics(com.example.datn.DTO.Response.ClassSectionCacheDTO theory, UUID currentSemesterId) {
        if (!theory.getSemesterId().equals(currentSemesterId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp học phần không thuộc học kỳ hiện tại");
        }
        if (theory.getParentSectionId() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã ID phải là của lớp lý thuyết gốc");
        }
    }

    private com.example.datn.DTO.Response.ClassSectionCacheDTO validateAndGetLabSection(EnrollRequest request, com.example.datn.DTO.Response.ClassSectionCacheDTO theory) {
        if (request.getLabClassId() != null) {
            com.example.datn.DTO.Response.ClassSectionCacheDTO lab = getFromRedisCache("class_metadata:" + request.getLabClassId());
            if (lab == null) {
                throw new AppException(ErrorCode.NO_CLASS_SECTIONS_FOUND, "Không tìm thấy lớp thực hành");
            }
            if (lab.getParentSectionId() == null || !lab.getParentSectionId().equals(theory.getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp thực hành không thuộc về lớp lý thuyết");
            }
            return lab;
        } else if (theory.isHasLab()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Môn học này yêu cầu chọn 1 ca thực hành");
        }
        return null;
    }





    // ĐÃ SỬA: TỰ CẤP PHÁT UUID VÀ BỎ HOÀN TOÀN TRUY VẤN DB TRONG prepareEnrollment
    private Enrollment prepareEnrollment(Student student, com.example.datn.DTO.Response.ClassSectionCacheDTO dto) {
        ClassSection proxy = new ClassSection();
        proxy.setId(dto.getId());
        
        Subject subjectProxy = new Subject();
        subjectProxy.setId(dto.getSubjectId());
        subjectProxy.setCode(dto.getSubjectCode());
        subjectProxy.setName(dto.getSubjectName());
        proxy.setSubject(subjectProxy);

        com.example.datn.Model.Semester semesterProxy = new com.example.datn.Model.Semester();
        semesterProxy.setId(dto.getSemesterId());
        proxy.setSemester(semesterProxy);

        return Enrollment.builder()
                .id(UUID.randomUUID()) // Cấp phát UUID ngay tại đây để DTO không bị lỗi
                .student(student)
                .classSection(proxy)
                .status(EnrollmentStatus.REGISTERED)
                .enrollmentDate(LocalDateTime.now())
                .build();
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
        String prereqKey = "prerequisites:" + subjectId;
        String passedKey = "passed_subjects:" + studentId;
        
        Set<String> prerequisites = redisTemplate.opsForSet().members(prereqKey);
        if (prerequisites == null || prerequisites.isEmpty()) {
            return;
        }

        for (String prereqId : prerequisites) {
            Boolean passed = redisTemplate.opsForSet().isMember(passedKey, prereqId);
            if (Boolean.FALSE.equals(passed)) {
                // Ta chỉ hiện ID môn vì không lưu name trên Redis, hoặc có thể query thêm nếu cần.
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Chưa đạt môn tiên quyết yêu cầu.");
            }
        }
    }

    @Override
    public void clearRedisDataAfterRegistration() {
        redisService.clearRegistrationData();
    }
}