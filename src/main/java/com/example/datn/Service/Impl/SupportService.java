package com.example.datn.Service.Impl;

import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.ENUM.*;
import com.example.datn.Config.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SupportService {
    private final StudentRepository studentRepository;
    private final JwtProvider jwtProvider;

    private final ScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final LecturerRepository lecturerRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final SemesterRepository semesterRepository;
    private final FacultyRepository facultyRepository;
    private final MajorRepository majorRepository;
    private final TypeRoomRepository typeRoomRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectComponentRepository subjectComponentRepository;

    public void createTokenCsvFile(int limit) {
        List<User> students = studentRepository.findAll()
                .stream()
                .filter(student -> student.getUser() != null)
                .map(Student::getUser)
                .limit(limit)
                .toList();

        String csvFilePath = "D:/user.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {
            for (User user : students) {
                String token = jwtProvider.genAccessToken(user.getUsername());
                writer.println(token);
            }
            log.info("Exported {} tokens to {}", students.size(), csvFilePath);
        } catch (IOException e) {
            log.error("Error writing CSV file", e);
        }
    }
    @Transactional
    public void generateStressTestData() {
        log.info("[StressTest] Bắt đầu xóa dữ liệu cũ để chuẩn bị test...");

        // 1. Xóa dữ liệu cũ theo đúng thứ tự ràng buộc khóa ngoại
        scheduleRepository.deleteAll();
        enrollmentRepository.deleteAll();
        classSectionRepository.deleteAll();
        
        // Xóa giảng viên
        List<Lecturer> testLecturers = lecturerRepository.findAll();
        lecturerRepository.deleteAll(testLecturers);
        
        // Xóa người dùng có tên bắt đầu bằng gv
        List<User> testUsers = userRepository.findAll().stream()
                .filter(u -> u.getUsername().startsWith("gv"))
                .toList();
        userRepository.deleteAll(testUsers);
        
        roomRepository.deleteAll();

        // Xóa SubjectComponent và Subject mẫu test cũ (để tránh rác và trùng lặp khóa ngoại)
        List<Subject> oldTestSubjects = subjectRepository.findAll().stream()
                .filter(s -> s.getCode().startsWith("MON_LT_") || s.getCode().startsWith("MON_TH_") 
                        || s.getCode().equals("MON_LT_TEST") || s.getCode().equals("MON_TH_TEST"))
                .toList();
        for (Subject sub : oldTestSubjects) {
            List<SubjectComponent> comps = subjectComponentRepository.findBySubjectId(sub.getId());
            subjectComponentRepository.deleteAll(comps);
        }
        subjectRepository.deleteAll(oldTestSubjects);

        log.info("[StressTest] Đã dọn dẹp sạch sẽ DB. Bắt đầu tạo mới tài nguyên...");

        // 2. Lấy thông tin nền
        Semester semester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ hiện tại trong DB. Vui lòng tạo 1 học kỳ current trước."));
        
        Faculty faculty = facultyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Chưa có Faculty nào trong DB. Vui lòng seed faculty trước."));
                
        Major major = majorRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Chưa có Major nào trong DB. Vui lòng seed major trước."));

        // 3. Lấy hoặc tạo RoomType
        RoomType practiceType = typeRoomRepository.findAll().stream()
                .filter(t -> "PRACTICE".equalsIgnoreCase(t.getCode()) || "LAB".equalsIgnoreCase(t.getCode()))
                .findFirst()
                .orElseGet(() -> typeRoomRepository.save(RoomType.builder().code("PRACTICE").name("Phòng máy thực hành").build()));

        RoomType theoryType = typeRoomRepository.findAll().stream()
                .filter(t -> "THEORY".equalsIgnoreCase(t.getCode()) || "NORMAL".equalsIgnoreCase(t.getCode()))
                .findFirst()
                .orElseGet(() -> typeRoomRepository.save(RoomType.builder().code("THEORY").name("Phòng lý thuyết thường").build()));

        // 4. Tạo 30 phòng học
        // 15 phòng máy ( PM01 -> PM15, capacity 40 )
        for (int i = 1; i <= 15; i++) {
            roomRepository.save(Room.builder()
                    .name(String.format("PM%02d", i))
                    .capacity(40)
                    .roomType(practiceType)
                    .build());
        }
        // 2 phòng thường 120 chỗ ( P120_1, P120_2 )
        for (int i = 1; i <= 2; i++) {
            roomRepository.save(Room.builder()
                    .name("P120_" + i)
                    .capacity(120)
                    .roomType(theoryType)
                    .build());
        }
        // 5 phòng thường 80 chỗ ( P80_1 -> P80_5 )
        for (int i = 1; i <= 5; i++) {
            roomRepository.save(Room.builder()
                    .name("P80_" + i)
                    .capacity(80)
                    .roomType(theoryType)
                    .build());
        }
        // 8 phòng thường 40 chỗ ( P40_1 -> P40_8 )
        for (int i = 1; i <= 8; i++) {
            roomRepository.save(Room.builder()
                    .name("P40_" + i)
                    .capacity(40)
                    .roomType(theoryType)
                    .build());
        }

        // 5. Tạo 40 giảng viên ( gv1 -> gv40 )
        for (int i = 1; i <= 40; i++) {
            String username = "gv" + i;
            User user = User.builder()
                    .username(username)
                    .password("$2a$10$e0MYzAdy8Q1Y9aV.24Q.N.343/c7Z5M83.4739572.846/9284242") // BCrypt cho "123456"
                    .fullName("Giảng viên " + i)
                    .email(username + "@test.com")
                    .isActive(true)
                    .build();
            user = userRepository.save(user);

            Lecturer lecturer = Lecturer.builder()
                    .lecturerCode(String.format("GV%03d", i))
                    .fullName(user.getFullName())
                    .user(user)
                    .gender(Gender.MALE)
                    .status(LecturerStatus.WORKING)
                    .faculty(faculty)
                    .major(major)
                    .build();
            lecturerRepository.save(lecturer);
        }

        // 6. Lấy hoặc tạo Môn học mẫu (2 tiết và 3 tiết)
        Subject theorySub3 = subjectRepository.findByCode("MON_LT_3TC")
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .code("MON_LT_3TC")
                        .name("Môn Lý Thuyết 3TC")
                        .credits(3)
                        .departmentName(major.getName())
                        .isActive(true)
                        .totalPeriods(45)
                        .build()));

        Subject theorySub2 = subjectRepository.findByCode("MON_LT_2TC")
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .code("MON_LT_2TC")
                        .name("Môn Lý Thuyết 2TC")
                        .credits(2)
                        .departmentName(major.getName())
                        .isActive(true)
                        .totalPeriods(30)
                        .build()));

        Subject practiceSub3 = subjectRepository.findByCode("MON_TH_3TC")
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .code("MON_TH_3TC")
                        .name("Môn Thực Hành 3TC")
                        .credits(3)
                        .departmentName(major.getName())
                        .isActive(true)
                        .totalPeriods(45)
                        .build()));

        Subject practiceSub2 = subjectRepository.findByCode("MON_TH_2TC")
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .code("MON_TH_2TC")
                        .name("Môn Thực Hành 2TC")
                        .credits(2)
                        .departmentName(major.getName())
                        .isActive(true)
                        .totalPeriods(30)
                        .build()));

        // Cấu phần môn học
        SubjectComponent theoryComp3 = subjectComponentRepository.findBySubjectId(theorySub3.getId()).stream()
                .filter(c -> c.getType() == ComponentType.THEORY)
                .findFirst()
                .orElseGet(() -> subjectComponentRepository.save(SubjectComponent.builder()
                        .subject(theorySub3)
                        .type(ComponentType.THEORY)
                        .requiredRoomType(theoryType)
                        .sessionsPerWeek(1)
                        .periodsPerSession(3)
                        .totalPeriods(45)
                        .build()));

        SubjectComponent theoryComp2 = subjectComponentRepository.findBySubjectId(theorySub2.getId()).stream()
                .filter(c -> c.getType() == ComponentType.THEORY)
                .findFirst()
                .orElseGet(() -> subjectComponentRepository.save(SubjectComponent.builder()
                        .subject(theorySub2)
                        .type(ComponentType.THEORY)
                        .requiredRoomType(theoryType)
                        .sessionsPerWeek(1)
                        .periodsPerSession(2)
                        .totalPeriods(30)
                        .build()));

        SubjectComponent practiceComp3 = subjectComponentRepository.findBySubjectId(practiceSub3.getId()).stream()
                .filter(c -> c.getType() == ComponentType.PRACTICE)
                .findFirst()
                .orElseGet(() -> subjectComponentRepository.save(SubjectComponent.builder()
                        .subject(practiceSub3)
                        .type(ComponentType.PRACTICE)
                        .requiredRoomType(practiceType)
                        .sessionsPerWeek(1)
                        .periodsPerSession(3)
                        .totalPeriods(45)
                        .build()));

        SubjectComponent practiceComp2 = subjectComponentRepository.findBySubjectId(practiceSub2.getId()).stream()
                .filter(c -> c.getType() == ComponentType.PRACTICE)
                .findFirst()
                .orElseGet(() -> subjectComponentRepository.save(SubjectComponent.builder()
                        .subject(practiceSub2)
                        .type(ComponentType.PRACTICE)
                        .requiredRoomType(practiceType)
                        .sessionsPerWeek(1)
                        .periodsPerSession(2)
                        .totalPeriods(30)
                        .build()));

        // 7. Tạo 600 lớp học phần (300 thực hành, 30 lý thuyết lớn, 70 lý thuyết trung bình, 200 lý thuyết nhỏ)
        int sectionCounter = 1;
        List<ClassSection> sectionsToSave = new ArrayList<>();
        
        // 300 lớp thực hành (sĩ số 40, phòng máy): 150 lớp 3TC (3 tiết), 150 lớp 2TC (2 tiết)
        for (int i = 0; i < 150; i++) {
            sectionsToSave.add(ClassSection.builder()
                    .sectionCode(String.format("LHP_TH_3TC_%04d", sectionCounter++))
                    .subject(practiceSub3)
                    .subjectComponent(practiceComp3)
                    .semester(semester)
                    .capacity(40)
                    .status(SectionStatus.PENDING)
                    .build());
        }
        for (int i = 0; i < 150; i++) {
            sectionsToSave.add(ClassSection.builder()
                    .sectionCode(String.format("LHP_TH_2TC_%04d", sectionCounter++))
                    .subject(practiceSub2)
                    .subjectComponent(practiceComp2)
                    .semester(semester)
                    .capacity(40)
                    .status(SectionStatus.PENDING)
                    .build());
        }

        // 30 lớp lý thuyết lớn (sĩ số 120): 15 lớp 3TC (3 tiết), 15 lớp 2TC (2 tiết)
        for (int i = 0; i < 15; i++) {
            sectionsToSave.add(ClassSection.builder()
                    .sectionCode(String.format("LHP_LT_LON_3TC_%04d", sectionCounter++))
                    .subject(theorySub3)
                    .subjectComponent(theoryComp3)
                    .semester(semester)
                    .capacity(120)
                    .status(SectionStatus.PENDING)
                    .build());
        }
        for (int i = 0; i < 15; i++) {
            sectionsToSave.add(ClassSection.builder()
                    .sectionCode(String.format("LHP_LT_LON_2TC_%04d", sectionCounter++))
                    .subject(theorySub2)
                    .subjectComponent(theoryComp2)
                    .semester(semester)
                    .capacity(120)
                    .status(SectionStatus.PENDING)
                    .build());
        }

        // 70 lớp lý thuyết trung bình (sĩ số 80): 35 lớp 3TC (3 tiết), 35 lớp 2TC (2 tiết)
        for (int i = 0; i < 35; i++) {
            sectionsToSave.add(ClassSection.builder()
                    .sectionCode(String.format("LHP_LT_VUA_3TC_%04d", sectionCounter++))
                    .subject(theorySub3)
                    .subjectComponent(theoryComp3)
                    .semester(semester)
                    .capacity(80)
                    .status(SectionStatus.PENDING)
                    .build());
        }
        for (int i = 0; i < 35; i++) {
            sectionsToSave.add(ClassSection.builder()
                    .sectionCode(String.format("LHP_LT_VUA_2TC_%04d", sectionCounter++))
                    .subject(theorySub2)
                    .subjectComponent(theoryComp2)
                    .semester(semester)
                    .capacity(80)
                    .status(SectionStatus.PENDING)
                    .build());
        }

        // 200 lớp lý thuyết nhỏ (sĩ số 40): 100 lớp 3TC (3 tiết), 100 lớp 2TC (2 tiết)
        for (int i = 0; i < 100; i++) {
            sectionsToSave.add(ClassSection.builder()
                    .sectionCode(String.format("LHP_LT_NHO_3TC_%04d", sectionCounter++))
                    .subject(theorySub3)
                    .subjectComponent(theoryComp3)
                    .semester(semester)
                    .capacity(40)
                    .status(SectionStatus.PENDING)
                    .build());
        }
        for (int i = 0; i < 100; i++) {
            sectionsToSave.add(ClassSection.builder()
                    .sectionCode(String.format("LHP_LT_NHO_2TC_%04d", sectionCounter++))
                    .subject(theorySub2)
                    .subjectComponent(theoryComp2)
                    .semester(semester)
                    .capacity(40)
                    .status(SectionStatus.PENDING)
                    .build());
        }

        List<ClassSection> savedSections = classSectionRepository.saveAll(sectionsToSave);

        // Khởi tạo sẵn lịch học trống (schedules) cho tất cả các lớp học phần mới tạo
        List<Schedule> schedulesToSave = savedSections.stream()
                .map(section -> Schedule.builder().classSection(section).build())
                .collect(Collectors.toList());
        scheduleRepository.saveAll(schedulesToSave);

        log.info("[StressTest] Tạo dữ liệu test stress thành công! Tổng cộng: 30 phòng, 40 giảng viên, 600 lớp học phần (đã khởi tạo sẵn Schedule trống).");
    }
}