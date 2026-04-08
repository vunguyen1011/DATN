package com.example.datn.Service.Impl;

import com.example.datn.ENUM.Gender;
import com.example.datn.ENUM.LecturerStatus;
import com.example.datn.ENUM.StudentStatus;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IExcelService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelService implements IExcelService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final MajorRepository majorRepository;
    private final AdminClassRepository adminClassRepository;
    private final CohortRepository cohortRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final FacultyRepository facultyRepository;
    private   final LecturerRepository lecturerRepository;

    private static final String[] HEADERS = {
            "STT", "Mã sinh viên", "Họ và tên", "Giới tính", "Số điện thoại", "Địa chỉ", "Tên Lớp", "Mã Ngành", "Tên Khóa"
    };
    private static final String[] LECTURER_HEADERS = {
            "STT", "Mã giảng viên", "Họ và tên", "Ngày sinh (dd/MM/yyyy)", "Giới tính",
            "Số điện thoại", "Địa chỉ", "Học vị", "Mã Khoa", "Mã Ngành"
    };
    private static final String LECTURER_SHEET_NAME = "Danh_Sach_Giang_Vien";
    private static final String SHEET_NAME = "Danh_Sach_Sinh_Vien";

    @Override
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet hiddenSheet = workbook.createSheet("HiddenData");
            List<String> classes = adminClassRepository.findAll().stream().map(AdminClass::getName).collect(Collectors.toList());
            List<String> majors = majorRepository.findAll().stream().map(Major::getCode).collect(Collectors.toList());
            List<String> cohorts = cohortRepository.findAll().stream().map(Cohort::getName).collect(Collectors.toList());
            List<String> genders = Arrays.asList("Nam", "Nữ", "Khác");

            if (classes.isEmpty()) classes.add("CHUA_CO_LOP");
            if (majors.isEmpty()) majors.add("CHUA_CO_NGANH");
            if (cohorts.isEmpty()) cohorts.add("CHUA_CO_KHOA");

            int maxRows = Math.max(Math.max(classes.size(), majors.size()), Math.max(genders.size(), cohorts.size()));
            for (int i = 0; i < maxRows; i++) {
                Row row = hiddenSheet.createRow(i);
                if (i < classes.size()) row.createCell(0).setCellValue(classes.get(i));
                if (i < majors.size()) row.createCell(1).setCellValue(majors.get(i));
                if (i < genders.size()) row.createCell(2).setCellValue(genders.get(i));
                if (i < cohorts.size()) row.createCell(3).setCellValue(cohorts.get(i));
            }

            createNamedRange(workbook, "ClassList", 0, classes.size());
            createNamedRange(workbook, "MajorList", 1, majors.size());
            createNamedRange(workbook, "GenderList", 2, genders.size());
            createNamedRange(workbook, "CohortList", 3, cohorts.size());

            workbook.setSheetVisibility(workbook.getSheetIndex("HiddenData"), SheetVisibility.VERY_HIDDEN);

            Sheet sheet = workbook.createSheet(SHEET_NAME);
            workbook.setActiveSheet(workbook.getSheetIndex(SHEET_NAME));

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle unlockedStyle = workbook.createCellStyle();
            unlockedStyle.setLocked(false);

            CellStyle unlockedTextStyle = workbook.createCellStyle();
            unlockedTextStyle.setLocked(false);
            unlockedTextStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));

            CellStyle lockedSTTStyle = workbook.createCellStyle();
            lockedSTTStyle.setLocked(true);
            lockedSTTStyle.setAlignment(HorizontalAlignment.CENTER);
            lockedSTTStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            lockedSTTStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                if (i == 0) sheet.setColumnWidth(i, 6 * 256);
                else if (i == 5) sheet.setColumnWidth(i, 40 * 256);
                else sheet.setColumnWidth(i, 18 * 256);
            }

            for (int i = 1; i <= 5000; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < HEADERS.length; j++) {
                    Cell cell = row.createCell(j);
                    if (j == 0) {
                        cell.setCellStyle(lockedSTTStyle);
                        cell.setCellValue(i);
                    } else if (j == 1 || j == 4) {
                        cell.setCellStyle(unlockedTextStyle);
                    } else {
                        cell.setCellStyle(unlockedStyle);
                    }
                }
            }

            DataValidationHelper helper = sheet.getDataValidationHelper();
            addValidation(sheet, helper, "GenderList", 3);
            addValidation(sheet, helper, "ClassList", 6);
            addValidation(sheet, helper, "MajorList", 7);
            addValidation(sheet, helper, "CohortList", 8);

            sheet.protectSheet("admin_datn_2026");
            workbook.write(response.getOutputStream());
        }
    }

    @Override
    @Transactional
    public String saveUsersFromExcel(MultipartFile file) {
        if (!hasExcelFormat(file)) throw new AppException(ErrorCode.INVALID_FILE_FORMAT);

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) throw new AppException(ErrorCode.EXCEL_HEADER_MISMATCH);

            DataFormatter formatter = new DataFormatter();
            if (!isValidHeader(sheet.getRow(0))) throw new AppException(ErrorCode.EXCEL_HEADER_MISMATCH);

            List<String> usernamesInExcel = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                String code = formatter.formatCellValue(row.getCell(1)).trim().toUpperCase();
                if (!code.isEmpty()) usernamesInExcel.add(code);
            }

            Set<String> existingUsernames = new HashSet<>(userRepository.findUsernamesByUsernameIn(usernamesInExcel));
            Map<String, Major> majorMap = majorRepository.findAll().stream().collect(Collectors.toMap(Major::getCode, m -> m, (e, r) -> e));
            Map<String, AdminClass> classMap = adminClassRepository.findAll().stream().collect(Collectors.toMap(AdminClass::getName, c -> c, (e, r) -> e));
            Map<String, Cohort> cohortMap = cohortRepository.findAll().stream().collect(Collectors.toMap(Cohort::getName, c -> c, (e, r) -> e));

            Role studentRole = roleRepository.findByName("ROLE_USER").orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            List<User> usersToSave = new ArrayList<>();
            List<Student> studentsToSave = new ArrayList<>();
            List<UserRole> userRolesToSave = new ArrayList<>();
            List<String> skippedStudents = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String studentCode = formatter.formatCellValue(row.getCell(1)).trim().toUpperCase();
                if (studentCode.isEmpty()) break;

                if (existingUsernames.contains(studentCode)) {
                    skippedStudents.add(studentCode + " (Trùng mã)");
                    continue;
                }

                String fullName = formatter.formatCellValue(row.getCell(2)).trim();
                String genderStr = formatter.formatCellValue(row.getCell(3)).trim();
                String phone = formatter.formatCellValue(row.getCell(4)).trim();

                if (phone.length() == 9 && !phone.startsWith("0")) phone = "0" + phone;

                String address = formatter.formatCellValue(row.getCell(5)).trim();
                String className = formatter.formatCellValue(row.getCell(6)).trim();
                String majorCode = formatter.formatCellValue(row.getCell(7)).trim();
                String cohortName = formatter.formatCellValue(row.getCell(8)).trim();

                Major major = majorMap.get(majorCode);
                AdminClass adminClass = classMap.get(className);
                Cohort cohort = cohortMap.get(cohortName);

                if (major == null || adminClass == null || cohort == null) {
                    skippedStudents.add(studentCode + " (Lỗi Lớp/Ngành/Khóa)");
                    continue;
                }

                Gender genderEnum = genderStr.equalsIgnoreCase("Nam") ? Gender.MALE : (genderStr.equalsIgnoreCase("Nữ") ? Gender.FEMALE : Gender.OTHER);

                User newUser = User.builder()
                        .username(studentCode)
                        .fullName(fullName)
                        .email(studentCode.toLowerCase() + "@thanglong.edu.vn")
                        .password(passwordEncoder.encode(studentCode))
                        .isActive(true)
                        .isLocked(false)
                        .build();

                Student newStudent = Student.builder()
                        .studentCode(studentCode)
                        .fullName(fullName)
                        .gender(genderEnum)
                        .phone(phone)
                        .address(address)
                        .status(StudentStatus.STUDYING)
                        .major(major)
                        .adminClass(adminClass)
                        .cohort(cohort)
                        .user(newUser)
                        .build();

                UserRole roleMap = UserRole.builder().user(newUser).role(studentRole).build();

                usersToSave.add(newUser);
                studentsToSave.add(newStudent);
                userRolesToSave.add(roleMap);
                existingUsernames.add(studentCode);
            }

            if (!usersToSave.isEmpty()) {
                userRepository.saveAll(usersToSave);
                userRoleRepository.saveAll(userRolesToSave);
                studentRepository.saveAll(studentsToSave);
            }

            return skippedStudents.isEmpty() ?
                    "Import thành công " + studentsToSave.size() + " sinh viên." :
                    "Thành công " + studentsToSave.size() + ". Bỏ qua " + skippedStudents.size() + ": " + String.join(", ", skippedStudents);

        } catch (Exception e) {
            log.error("Lỗi Import: ", e);
            throw new AppException(ErrorCode.EXCEL_READ_ERROR);
        }
    }
@Override
    public void downloadTemplateLecturer(HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet hiddenSheet = workbook.createSheet("HiddenData");
            List<String> faculties = facultyRepository.findAll().stream().map(Faculty::getCode).collect(Collectors.toList());
            List<String> majors = majorRepository.findAll().stream().map(Major::getCode).collect(Collectors.toList());
            List<String> degrees = Arrays.asList("Cử nhân", "Thạc sĩ", "Tiến sĩ", "PGS.TS", "GS.TS");
            List<String> genders = Arrays.asList("Nam", "Nữ", "Khác");
            if (faculties.isEmpty()) faculties.add("CHUA_CO_KHOA");
            if (majors.isEmpty()) majors.add("CHUA_CO_NGANH");
            int maxRows = Math.max(Math.max(faculties.size(), majors.size()), Math.max(degrees.size(), genders.size()));
            for (int i = 0; i < maxRows; i++) {
                Row row = hiddenSheet.createRow(i);
                if (i < faculties.size()) row.createCell(0).setCellValue(faculties.get(i));
                if (i < majors.size()) row.createCell(1).setCellValue(majors.get(i));
                if (i < degrees.size()) row.createCell(2).setCellValue(degrees.get(i));
                if (i < genders.size()) row.createCell(3).setCellValue(genders.get(i));
            }
            createNamedRange(workbook, "FacultyList", 0, faculties.size());
            createNamedRange(workbook, "MajorList_Lec", 1, majors.size());
            createNamedRange(workbook, "DegreeList", 2, degrees.size());
            createNamedRange(workbook, "GenderList_Lec", 3, genders.size());
            workbook.setSheetVisibility(workbook.getSheetIndex("HiddenData"), SheetVisibility.VERY_HIDDEN);

            Sheet sheet = workbook.createSheet(LECTURER_SHEET_NAME);
            workbook.setActiveSheet(workbook.getSheetIndex(LECTURER_SHEET_NAME));

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle unlockedStyle = workbook.createCellStyle();
            unlockedStyle.setLocked(false);

            CellStyle unlockedTextStyle = workbook.createCellStyle();
            unlockedTextStyle.setLocked(false);
            unlockedTextStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));

            // Format cột ngày sinh
            CellStyle unlockedDateStyle = workbook.createCellStyle();
            unlockedDateStyle.setLocked(false);
            unlockedDateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));

            CellStyle lockedSTTStyle = workbook.createCellStyle();
            lockedSTTStyle.setLocked(true);
            lockedSTTStyle.setAlignment(HorizontalAlignment.CENTER);
            lockedSTTStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            lockedSTTStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < LECTURER_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(LECTURER_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                if (i == 0) sheet.setColumnWidth(i, 6 * 256);
                else if (i == 3) sheet.setColumnWidth(i, 20 * 256); // Ngày sinh
                else if (i == 6) sheet.setColumnWidth(i, 40 * 256); // Địa chỉ
                else sheet.setColumnWidth(i, 18 * 256);
            }

            for (int i = 1; i <= 1000; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < LECTURER_HEADERS.length; j++) {
                    Cell cell = row.createCell(j);
                    if (j == 0) {
                        cell.setCellStyle(lockedSTTStyle);
                        cell.setCellValue(i);
                    } else if (j == 1 || j == 5) {
                        cell.setCellStyle(unlockedTextStyle);
                    } else if (j == 3) { // Cột ngày sinh
                        cell.setCellStyle(unlockedDateStyle);
                    } else {
                        cell.setCellStyle(unlockedStyle);
                    }
                }
            }

            DataValidationHelper helper = sheet.getDataValidationHelper();
            addValidation(sheet, helper, "GenderList_Lec", 4); // Cột E
            addValidation(sheet, helper, "DegreeList", 7);     // Cột H
            addValidation(sheet, helper, "FacultyList", 8);    // Cột I
            addValidation(sheet, helper, "MajorList_Lec", 9);  // Cột J

            sheet.protectSheet("admin_datn_2026");
            workbook.write(response.getOutputStream());

        }
    }
    @Override
    @Transactional
    public String saveLecturersFromExcel(MultipartFile file) {
        if (!hasExcelFormat(file)) throw new AppException(ErrorCode.INVALID_FILE_FORMAT);

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(LECTURER_SHEET_NAME);
            if (sheet == null) throw new AppException(ErrorCode.EXCEL_HEADER_MISMATCH);

            DataFormatter formatter = new DataFormatter();
            if (!isValidLecturerHeader(sheet.getRow(0))) throw new AppException(ErrorCode.EXCEL_HEADER_MISMATCH);

            // Lấy danh sách username để check trùng
            List<String> usernamesInExcel = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;
                String code = formatter.formatCellValue(row.getCell(1)).trim().toUpperCase();
                if (!code.isEmpty()) usernamesInExcel.add(code);
            }

            Set<String> existingUsernames = new HashSet<>(userRepository.findUsernamesByUsernameIn(usernamesInExcel));

            // Map dữ liệu Khoa và Ngành
            Map<String, Faculty> facultyMap = facultyRepository.findAll().stream()
                    .collect(Collectors.toMap(Faculty::getCode, f -> f, (e, r) -> e));
            Map<String, Major> majorMap = majorRepository.findAll().stream()
                    .collect(Collectors.toMap(Major::getCode, m -> m, (e, r) -> e));

            // Lấy Role cho Giảng viên (Chú ý: tên Role trong DB của bạn phải khớp, ví dụ: ROLE_LECTURER hoặc ROLE_TEACHER)
            Role lecturerRole = roleRepository.findByName("ROLE_LECTURER")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            List<User> usersToSave = new ArrayList<>();
            List<Lecturer> lecturersToSave = new ArrayList<>();
            List<UserRole> userRolesToSave = new ArrayList<>();
            List<String> skippedLecturers = new ArrayList<>();

            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String lecturerCode = formatter.formatCellValue(row.getCell(1)).trim().toUpperCase();
                if (lecturerCode.isEmpty()) break;

                if (existingUsernames.contains(lecturerCode)) {
                    skippedLecturers.add(lecturerCode + " (Trùng mã)");
                    continue;
                }

                String fullName = formatter.formatCellValue(row.getCell(2)).trim();

                // Xử lý Ngày sinh
                LocalDate dateOfBirth = null;
                Cell dobCell = row.getCell(3);
                if (dobCell != null) {
                    try {
                        if (DateUtil.isCellDateFormatted(dobCell)) {
                            dateOfBirth = dobCell.getLocalDateTimeCellValue().toLocalDate();
                        } else {
                            String dobStr = formatter.formatCellValue(dobCell).trim();
                            if (!dobStr.isEmpty()) {
                                dateOfBirth = LocalDate.parse(dobStr, dateFormatter);
                            }
                        }
                    } catch (Exception e) {
                        skippedLecturers.add(lecturerCode + " (Sai format Ngày sinh)");
                        continue; // Bỏ qua GV này nếu format ngày sinh sai
                    }
                }

                String genderStr = formatter.formatCellValue(row.getCell(4)).trim();
                String phone = formatter.formatCellValue(row.getCell(5)).trim();
                String address = formatter.formatCellValue(row.getCell(6)).trim();
                String degree = formatter.formatCellValue(row.getCell(7)).trim();
                String facultyCode = formatter.formatCellValue(row.getCell(8)).trim();
                String majorCode = formatter.formatCellValue(row.getCell(9)).trim();

                if (phone.length() == 9 && !phone.startsWith("0")) phone = "0" + phone;

                Faculty faculty = facultyMap.get(facultyCode);
                Major major = majorMap.get(majorCode);

                if (faculty == null || major == null) {
                    skippedLecturers.add(lecturerCode + " (Lỗi mã Khoa/Ngành)");
                    continue;
                }

                Gender genderEnum = genderStr.equalsIgnoreCase("Nam") ? Gender.MALE : (genderStr.equalsIgnoreCase("Nữ") ? Gender.FEMALE : Gender.OTHER);

                // Mật khẩu gốc để gửi email
                String rawPassword = lecturerCode;

                User newUser = User.builder()
                        .username(lecturerCode)
                        .fullName(fullName)
                        .email(lecturerCode.toLowerCase() + "@thanglong.edu.vn") // Domain email GV
                        .password(passwordEncoder.encode(rawPassword))
                        .isActive(true)
                        .isLocked(false)
                        .build();

                Lecturer newLecturer = Lecturer.builder()
                        .lecturerCode(lecturerCode)
                        .user(newUser)
                        .fullName(fullName)
                        .phone(phone)
                        .address(address)
                        .degree(degree)
                        .gender(genderEnum)
                        .dateOfBirth(dateOfBirth)
                        .status(LecturerStatus.WORKING)
                        .faculty(faculty)
                        .major(major)
                        .build();

                UserRole roleMap = UserRole.builder().user(newUser).role(lecturerRole).build();

                usersToSave.add(newUser);
                lecturersToSave.add(newLecturer);
                userRolesToSave.add(roleMap);
                existingUsernames.add(lecturerCode);
            }

            if (!usersToSave.isEmpty()) {
                userRepository.saveAll(usersToSave);
                userRoleRepository.saveAll(userRolesToSave);
                lecturerRepository.saveAll(lecturersToSave);


                }


            return skippedLecturers.isEmpty() ?
                    "Import thành công " + lecturersToSave.size() + " giảng viên." :
                    "Thành công " + lecturersToSave.size() + ". Bỏ qua " + skippedLecturers.size() + ": " + String.join(", ", skippedLecturers);

        } catch (Exception e) {
            log.error("Lỗi Import Giảng viên: ", e);
            throw new AppException(ErrorCode.EXCEL_READ_ERROR);
        }
    }

    // Cần thêm hàm này vì hàm isValidHeader cũ đang cứng bằng biến HEADERS của sinh viên
    private boolean isValidLecturerHeader(Row row) {
        if (row == null) return false;
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < LECTURER_HEADERS.length; i++) {
            if (!LECTURER_HEADERS[i].equalsIgnoreCase(formatter.formatCellValue(row.getCell(i)).trim())) return false;
        }
        return true;
    }
    private void createNamedRange(Workbook wb, String name, int col, int size) {
        Name namedRange = wb.createName();
        namedRange.setNameName(name);
        String colLetter = String.valueOf((char)('A' + col));
        namedRange.setRefersToFormula("HiddenData!$" + colLetter + "$1:$" + colLetter + "$" + size);
    }

    private void addValidation(Sheet sheet, DataValidationHelper helper, String formula, int col) {
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, 5000, col, col));
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font); style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND); style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private boolean isValidHeader(Row row) {
        if (row == null) return false;
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < HEADERS.length; i++) {
            if (!HEADERS[i].equalsIgnoreCase(formatter.formatCellValue(row.getCell(i)).trim())) return false;
        }
        return true;
    }

    public boolean hasExcelFormat(MultipartFile file) {
        String type = file.getContentType();
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(type)
                || "application/vnd.ms-excel".equals(type);
    }
}