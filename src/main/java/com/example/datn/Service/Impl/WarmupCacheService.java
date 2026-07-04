package com.example.datn.Service.Impl;

import com.example.datn.ENUM.EnrollmentStatus;
import com.example.datn.Model.ClassSection;
import com.example.datn.Model.Enrollment;
import com.example.datn.Model.Schedule;
import com.example.datn.Model.Student;
import com.example.datn.Model.Subject;
import com.example.datn.Repository.ClassSectionRepository;
import com.example.datn.Repository.EnrollmentRepository;
import com.example.datn.Repository.ScheduleRepository;
import com.example.datn.Repository.StudentRepository;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Service.Interface.IStudentGradeService;
import com.example.datn.Service.Interface.ISubjectService;
import com.example.datn.Service.Interface.IWarmupCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarmupCacheService implements IWarmupCacheService, CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(readOnly = true)
    public void run(String... args) throws Exception {
        log.info("--- Auto-running Warmup Cache on Startup ---");
        try {
            warmupAll();
        } catch (Exception e) {
            log.error("Lỗi khi tự động Warmup Cache: ", e);
        }
    }
    
    private final ClassSectionRepository classSectionRepository;
    private final StudentRepository studentRepository;
    private final ScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final com.example.datn.Repository.SemesterRepository semesterRepository;
    private final com.example.datn.Repository.PeriodCohortRepository periodCohortRepository;
    
    private final IStudentGradeService studentGradeService;
    private final ISubjectService subjectService;
    private final LocalCacheService localCacheService;

    @Override
    public void warmupAll() {
        log.info("Bắt đầu quá trình Warmup Cache (Endgame Architecture - Zero DB I/O)...");
        
        warmupPassedSubjects();
        warmupPrerequisites();
        warmupClassMasks();
        warmupStudentMasks();
        warmupClassMetadata();
        warmupStudentUsernames();
        
        localCacheService.logStats();
        log.info("Hoàn tất Warmup Cache!");
    }

    private void warmupPassedSubjects() {
        log.info("Đang tính toán Passed Subjects...");
        List<Student> students = studentRepository.findAll();
        for (Student student : students) {
            Set<UUID> passed = studentGradeService.getPassedSubjectIds(student.getId());
            String key = "passed_subjects:" + student.getId();
            redisTemplate.delete(key);
            if (!passed.isEmpty()) {
                String[] values = passed.stream().map(UUID::toString).toArray(String[]::new);
                redisTemplate.opsForSet().add(key, values);
            }
            localCacheService.putPassedSubjects(student.getId(), passed);
        }
        log.info("Đã cache điểm đã qua cho {} sinh viên.", students.size());
    }

    private void warmupPrerequisites() {
        log.info("Đang tính toán Prerequisites...");
        List<Subject> subjects = subjectRepository.findAll();
        for (Subject subject : subjects) {

            // Bỏ qua các môn học đã bị xóa mềm (inactive) để tránh lỗi SUBJECT_NOT_FOUND
            if (subject.getIsActive() == null || !subject.getIsActive()) {
                continue;
            }

            List<com.example.datn.DTO.Response.SubjectResponse> prereqs = subjectService.getPrerequisites(subject.getId());
            String key = "prerequisites:" + subject.getId();
            redisTemplate.delete(key);
            if (prereqs != null && !prereqs.isEmpty()) {
                String[] values = prereqs.stream().map(p -> p.getId().toString()).toArray(String[]::new);
                redisTemplate.opsForSet().add(key, values);
                Set<UUID> prereqIds = prereqs.stream().map(com.example.datn.DTO.Response.SubjectResponse::getId).collect(Collectors.toSet());
                localCacheService.putPrerequisites(subject.getId(), prereqIds);
            } else {
                localCacheService.putPrerequisites(subject.getId(), Collections.emptySet());
            }
        }
        log.info("Đã cache môn tiên quyết cho {} môn học.", subjects.size());
    }

    private void warmupClassMasks() {
        log.info("Đang tính toán Class Masks...");
        // Ở môi trường thực tế, ta chỉ lấy các lớp của học kỳ hiện tại đang mở đăng ký.
        // Tạm thời lấy hết để demo (hoặc bạn có thể dùng findBySemesterId)
        List<ClassSection> sections = classSectionRepository.findAll();
        
        for (ClassSection section : sections) {
            List<Schedule> schedules = scheduleRepository.findByClassSection_Id(section.getId());
            int[] mask = buildScheduleMask(schedules);
            String key = "class_mask:" + section.getId();
            try {
                String maskStr = objectMapper.writeValueAsString(mask);
                redisTemplate.opsForValue().set(key, maskStr);
                localCacheService.putClassMask(section.getId(), maskStr);
            } catch (Exception e) {
                log.error("Lỗi parse mask cho lớp {}", section.getId(), e);
            }
        }
        log.info("Đã cache bitmask lịch học cho {} lớp học phần.", sections.size());
    }

    private void warmupStudentMasks() {
        log.info("Đang tính toán Student Masks...");
        List<Student> students = studentRepository.findAll();
        
        com.example.datn.Model.Semester currentSemester = semesterRepository.findByIsCurrentTrue().orElse(null);
        if (currentSemester == null) {
            log.warn("Không tìm thấy Học kỳ hiện tại để tính Student Masks.");
            return;
        }
        
        for (Student student : students) {
            List<Enrollment> enrollments = enrollmentRepository.findActiveEnrollmentsBySemester(
                student.getId(), currentSemester.getId(), EnrollmentStatus.REGISTERED);
            // Collect tất cả lịch học của các lớp đã đăng ký
            List<Schedule> allSchedules = enrollments.stream()
                    .flatMap(en -> scheduleRepository.findByClassSection_Id(en.getClassSection().getId()).stream())
                    .collect(Collectors.toList());
                    
            int[] mask = buildScheduleMask(allSchedules);
            String key = "student_mask:" + student.getId();
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(mask));
            } catch (Exception e) {
                log.error("Lỗi parse mask cho sinh viên {}", student.getId(), e);
            }
        }
        log.info("Đã cache bitmask thời khóa biểu cho {} sinh viên.", students.size());
    }

    private void warmupClassMetadata() {
        log.info("Đang tính toán Class Metadata...");
        List<ClassSection> sections = classSectionRepository.findAll();
        for (ClassSection section : sections) {
            boolean hasLab = classSectionRepository.existsByParentSectionId(section.getId());
            com.example.datn.DTO.Response.ClassSectionCacheDTO dto = com.example.datn.DTO.Response.ClassSectionCacheDTO.builder()
                .id(section.getId())
                .subjectId(section.getSubject() != null ? section.getSubject().getId() : null)
                .subjectName(section.getSubject() != null ? section.getSubject().getName() : null)
                .subjectCode(section.getSubject() != null ? section.getSubject().getCode() : null)
                .semesterId(section.getSemester() != null ? section.getSemester().getId() : null)
                .parentSectionId(section.getParentSection() != null ? section.getParentSection().getId() : null)
                .hasLab(hasLab)
                .build();
            String key = "class_metadata:" + section.getId();
            try {
                String dtoStr = objectMapper.writeValueAsString(dto);
                redisTemplate.opsForValue().set(key, dtoStr);
                localCacheService.putClassMetadata(section.getId(), dto);
            } catch (Exception e) {
                log.error("Lỗi parse metadata cho lớp {}", section.getId(), e);
            }
        }
        log.info("Đã cache metadata cho {} lớp học phần.", sections.size());
    }



    // Thuật toán mượn từ RegistrationServiceImpl
    private int[] buildScheduleMask(List<Schedule> schedules) {
        int[] mask = new int[9];
        for (Schedule s : schedules) {
            if (s.getDayOfWeek() == null || s.getStartPeriod() == null || s.getEndPeriod() == null) continue;
            int day = s.getDayOfWeek();
            int start = s.getStartPeriod();
            int end = s.getEndPeriod();
            int periodMask = ((1 << (end - start + 1)) - 1) << start;
            mask[day] |= periodMask;
        }
        return mask;
    }

    private void warmupStudentUsernames() {
        log.info("Đang nạp thông tin Student theo Username vào Local Cache...");
        List<com.example.datn.Model.Student> students = studentRepository.findAll();
        for (com.example.datn.Model.Student student : students) {
            if (student.getUser() != null) {
                com.example.datn.Model.Student lightweight = new com.example.datn.Model.Student();
                lightweight.setId(student.getId());
                if (student.getCohort() != null) {
                    com.example.datn.Model.Cohort c = new com.example.datn.Model.Cohort();
                    c.setId(student.getCohort().getId());
                    lightweight.setCohort(c);
                }
                localCacheService.putStudentByUsername(student.getUser().getUsername(), lightweight);
            }
        }
    }
}
