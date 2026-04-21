package com.example.datn.Service.Impl;

import com.example.datn.DTO.Response.ClassSectionResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.ClassSection;
import com.example.datn.Model.Semester;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectComponent;
import com.example.datn.Repository.ClassSectionRepository;
import com.example.datn.Repository.SemesterRepository;
import com.example.datn.Repository.SubjectComponentRepository;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Service.Interface.IClassSectionService;
import com.example.datn.ENUM.ComponentType;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import com.example.datn.DTO.Response.SubjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassSectionServiceImpl implements IClassSectionService {


    private final ClassSectionRepository classSectionRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectComponentRepository subjectComponentRepository;
    private final com.example.datn.Mapper.SubjectMapper subjectMapper; // Đã thêm Mapper (có thể cần autowire hoặc inject trong constructor/Lombok)
    private final com.example.datn.Mapper.ClassSectionMapper classSectionMapper;

    private List<Subject> getAllSubjects() {
        return subjectRepository.findByIsActiveTrue(org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String[] columns = {"Tên môn học", "Mã môn học", "Số lượng lớp mở", "Sĩ số mỗi lớp", "Số lớp phụ / 1 lớp chính (Tự chọn)"};
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Class Sections Template");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);
        headerCellStyle.setLocked(true);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerCellStyle);
        }

        Sheet dataSheet = workbook.createSheet("SubjectData");
        List<Subject> subjects = getAllSubjects();
        for (int i = 0; i < subjects.size(); i++) {
            Row row = dataSheet.createRow(i);
            Subject subject = subjects.get(i);
            row.createCell(0).setCellValue(subject.getName());
            row.createCell(1).setCellValue(subject.getCode());
        }
        workbook.setSheetHidden(1, true);

        if (!subjects.isEmpty()) {
            DataValidationHelper validationHelper = sheet.getDataValidationHelper();
            String formula = "SubjectData!$A$1:$A$" + subjects.size();
            DataValidationConstraint constraint = validationHelper.createFormulaListConstraint(formula);
            CellRangeAddressList addressList = new CellRangeAddressList(1, 500, 0, 0);
            DataValidation validation = validationHelper.createValidation(constraint, addressList);
            validation.setShowErrorBox(true);
            sheet.addValidationData(validation);
        }

        CellStyle lockedStyle = workbook.createCellStyle();
        lockedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        lockedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        lockedStyle.setLocked(true);

        CellStyle unlockedStyle = workbook.createCellStyle();
        unlockedStyle.setLocked(false);

        for (int i = 1; i <= 500; i++) {
            Row row = sheet.createRow(i);

            row.createCell(0).setCellStyle(unlockedStyle);

            Cell codeCell = row.createCell(1);
            codeCell.setCellFormula(String.format("IF(ISBLANK(A%d), \"\", VLOOKUP(A%d, SubjectData!$A:$B, 2, FALSE))", i + 1, i + 1));
            codeCell.setCellStyle(lockedStyle);

            row.createCell(2).setCellStyle(unlockedStyle);
            row.createCell(3).setCellStyle(unlockedStyle);
            row.createCell(4).setCellStyle(unlockedStyle);
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.setColumnWidth(0, 256 * 40);

        sheet.protectSheet("tlu_vuvn");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=class_sections_template.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Override
    @Transactional
    public String importClassSections( MultipartFile file) {
        Semester semester = semesterRepository.findByIsCurrentTrue().orElseThrow(()-> new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));

        List<ClassSection> parentsToSave = new ArrayList<>();
        List<ClassSection> childrenToSave = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        int successRows = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new AppException(ErrorCode.EXCEL_FILE_EMPTY, "File Excel không có dữ liệu");
            }

            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            Map<String, Subject> subjectMap = subjectRepository.findByIsActiveTrue(org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
                    .collect(Collectors.toMap(Subject::getCode, s -> s));

            List<UUID> activeSubjectIds = subjectMap.values().stream().map(Subject::getId).collect(Collectors.toList());
            Map<UUID, List<SubjectComponent>> componentMap = new HashMap<>();

            if (!activeSubjectIds.isEmpty()) {
                componentMap = subjectComponentRepository.findBySubjectIdIn(activeSubjectIds).stream()
                        .collect(Collectors.groupingBy(c -> c.getSubject().getId()));
            }

            // Lấy tất cả danh sách lớp hiện có trong kỳ để check Suffix
            List<ClassSection> existingSectionsInSemester = classSectionRepository.findBySemesterId(semester.getId());

            // Dùng Map để lưu lại Max Suffix hiện tại của từng môn học
            Map<String, Integer> currentMaxParentSuffixMap = new HashMap<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row currentRow = sheet.getRow(rowIndex);
                if (currentRow == null) continue;

                int displayRow = rowIndex + 1;

                try {
                    Cell codeCell = currentRow.getCell(1);
                    if (codeCell == null || codeCell.getCellType() == CellType.BLANK || codeCell.getCellType() == CellType.ERROR) {
                        continue;
                    }

                    String subjectCode = dataFormatter.formatCellValue(codeCell, evaluator).trim();
                    if (subjectCode.isEmpty() || subjectCode.equals("#N/A")) {
                        continue;
                    }

                    int numberOfSections = parseIntegerCell(currentRow.getCell(2), dataFormatter, evaluator, "Số lượng lớp mở", displayRow);
                    if (numberOfSections <= 0) throw new AppException(ErrorCode.EXCEL_DATA_INVALID, "Số lượng lớp mở phải lớn hơn 0");

                    int capacity = parseIntegerCell(currentRow.getCell(3), dataFormatter, evaluator, "Sĩ số mỗi lớp", displayRow);
                    if (capacity <= 0) throw new AppException(ErrorCode.EXCEL_DATA_INVALID, "Sĩ số mỗi lớp phải lớn hơn 0");

                    int splitRatio = 1;
                    Cell ratioCell = currentRow.getCell(4);
                    if (ratioCell != null && ratioCell.getCellType() != CellType.BLANK) {
                        String ratioVal = dataFormatter.formatCellValue(ratioCell, evaluator).trim();
                        if (!ratioVal.isEmpty()) {
                            splitRatio = parseIntegerCell(ratioCell, dataFormatter, evaluator, "Số lớp phụ / 1 lớp chính (Tự chọn)", displayRow);
                        }
                    }
                    if (splitRatio <= 0) splitRatio = 1;

                    Subject subject = subjectMap.get(subjectCode);
                    if (subject == null) throw new AppException(ErrorCode.SUBJECT_NOT_FOUND, "Không tìm thấy mã môn: " + subjectCode);

                    List<SubjectComponent> components = componentMap.getOrDefault(subject.getId(), new ArrayList<>());
                    if (components.isEmpty()) throw new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND, "Môn học " + subjectCode + " chưa cấu hình thành phần môn");

                    List<ClassSection> rowParents = new ArrayList<>();
                    List<ClassSection> rowChildren = new ArrayList<>();

                    List<SubjectComponent> theories = components.stream().filter(c -> c.getType() == ComponentType.THEORY).collect(Collectors.toList());
                    List<SubjectComponent> practices = components.stream().filter(c -> c.getType() == ComponentType.PRACTICE).collect(Collectors.toList());

                    // Khởi tạo Max Suffix nếu môn này chưa từng được quét
                    if (!currentMaxParentSuffixMap.containsKey(subjectCode)) {
                        int max = getMaxSuffixForSubject(existingSectionsInSemester, subjectCode);
                        currentMaxParentSuffixMap.put(subjectCode, max);
                    }

                    // 1. Sinh các lớp Lý thuyết (Parent)
                    for (SubjectComponent theory : theories) {
                        for (int i = 0; i < numberOfSections; i++) {
                            // Cập nhật Suffix mới nhất (chống trùng lặp tuyệt đối)
                            int nextSuffix = currentMaxParentSuffixMap.get(subjectCode) + 1;
                            currentMaxParentSuffixMap.put(subjectCode, nextSuffix);

                            ClassSection section = ClassSection.builder()
                                    .sectionCode(String.format("%s-%02d", subjectCode, nextSuffix))
                                    .subject(subject)
                                    .subjectComponent(theory)
                                    .semester(semester)
                                    .capacity(capacity)
                                    .minStudents(0)
                                    .build();

                            rowParents.add(section);
                        }
                    }

                    // 2. Sinh các lớp Thực hành (Child)
                    for (SubjectComponent practice : practices) {
                        if (rowParents.isEmpty()) {
                            // Ngoại lệ: Môn chỉ có TH, không có LT
                            for (int i = 0; i < numberOfSections; i++) {
                                int nextSuffix = currentMaxParentSuffixMap.get(subjectCode) + 1;
                                currentMaxParentSuffixMap.put(subjectCode, nextSuffix);

                                ClassSection section = ClassSection.builder()
                                        .sectionCode(String.format("%s-%02d", subjectCode, nextSuffix))
                                        .subject(subject)
                                        .subjectComponent(practice)
                                        .semester(semester)
                                        .capacity(capacity / splitRatio)
                                        .minStudents(0)
                                        .build();
                                rowParents.add(section);
                            }
                        } else {
                            // Tự động bung Child từ Parent
                            for (ClassSection parent : rowParents) {
                                int baseCapacity = parent.getCapacity() / splitRatio; // Phần nguyên
                                int remainder = parent.getCapacity() % splitRatio;    // Phần dư (nếu chia lẻ)

                                for (int k = 0; k < splitRatio; k++) {
                                    int actualCapacity = baseCapacity;
                                    // Nhồi số sinh viên dư vào lớp thực hành cuối cùng
                                    if (k == splitRatio - 1) {
                                        actualCapacity += remainder;
                                    }

                                    ClassSection section = ClassSection.builder()
                                            .sectionCode(parent.getSectionCode() + "." + (k + 1))
                                            .subject(subject)
                                            .subjectComponent(practice)
                                            .parentSection(parent)
                                            .semester(semester)
                                            .capacity(actualCapacity)
                                            .minStudents(0)
                                            .build();
                                    rowChildren.add(section);
                                }
                            }
                        }
                    }

                    parentsToSave.addAll(rowParents);
                    childrenToSave.addAll(rowChildren);
                    successRows++;

                } catch (AppException e) {
                    errorMessages.add("Dòng " + displayRow + ": " + e.getMessage());
                } catch (Exception e) {
                    errorMessages.add("Dòng " + displayRow + ": Lỗi hệ thống (" + e.getMessage() + ")");
                }
            }

            int totalSaved = 0;

            if (!parentsToSave.isEmpty()) {
                classSectionRepository.saveAll(parentsToSave);
                totalSaved += parentsToSave.size();
            }

            if (!childrenToSave.isEmpty()) {
                classSectionRepository.saveAll(childrenToSave);
                totalSaved += childrenToSave.size();
            }

            if (totalSaved == 0 && !errorMessages.isEmpty()) {
                String limitedErrors = errorMessages.stream().limit(5).collect(Collectors.joining(" | "));
                String suffix = errorMessages.size() > 5 ? " ... (và " + (errorMessages.size() - 5) + " lỗi khác)" : "";
                throw new AppException(ErrorCode.EXCEL_DATA_INVALID, "Import thất bại toàn bộ. Lỗi chi tiết: " + limitedErrors + suffix);
            }

            StringBuilder resultMessage = new StringBuilder();
            resultMessage.append("Đã đọc hợp lệ ").append(successRows).append(" dòng. ");
            resultMessage.append("Tạo thành công ").append(totalSaved).append(" lớp học phần.");

            if (!errorMessages.isEmpty()) {
                String limitedErrors = errorMessages.stream().limit(5).collect(Collectors.joining(" | "));
                String suffix = errorMessages.size() > 5 ? " ... (và " + (errorMessages.size() - 5) + " lỗi khác)" : "";
                resultMessage.append(" | Có ").append(errorMessages.size()).append(" dòng bị lỗi hoặc bỏ qua: ").append(limitedErrors).append(suffix);
            }

            return resultMessage.toString();

        } catch (IOException e) {
            throw new AppException(ErrorCode.EXCEL_READ_ERROR, "Lỗi đọc file Excel: " + e.getMessage());
        }
    }

    private int parseIntegerCell(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator, String columnName, int row) {
        String cellValue = formatter.formatCellValue(cell, evaluator).trim();
        if (cellValue.isEmpty()) throw new AppException(ErrorCode.EXCEL_DATA_INVALID, "Cột [" + columnName + "] không được để trống");
        try {
            Number number = NumberFormat.getInstance(Locale.US).parse(cellValue);
            return number.intValue();
        } catch (ParseException e) {
            throw new AppException(ErrorCode.EXCEL_DATA_INVALID, "Cột [" + columnName + "] sai định dạng số (Giá trị nhập: " + cellValue + ")");
        }
    }

    // --- HÀM HELPER HỖ TRỢ TÌM SỐ THỨ TỰ LỚN NHẤT ĐANG CÓ TRONG DB ---
    private int getMaxSuffixForSubject(List<ClassSection> existingSections, String subjectCode) {
        int maxSuffix = 0;
        for (ClassSection cs : existingSections) {
            String code = cs.getSectionCode();
            // Lọc đúng mã môn đó và lọc bỏ các lớp con (chứa dấu '.')
            if (code != null && code.startsWith(subjectCode + "-") && !code.contains(".")) {
                try {
                    String suffixStr = code.substring(code.lastIndexOf("-") + 1);
                    int suffix = Integer.parseInt(suffixStr);
                    if (suffix > maxSuffix) {
                        maxSuffix = suffix;
                    }
                } catch (NumberFormatException ignored) {
                    // Bỏ qua rác
                }
            }
        }
        return maxSuffix;
    }

    @Override
    public List<SubjectResponse> getOpenedSubjectsBySemester(UUID semesterId) {
        return classSectionRepository.findDistinctSubjectsBySemesterId(semesterId).stream()
                .map(subjectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public com.example.datn.DTO.Response.ClassSectionResponse updateClassSection(UUID id, com.example.datn.DTO.Request.ClassSectionUpdateRequest request) {
        ClassSection section = classSectionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND, "Không tìm thấy lớp học phần"));

        if (request.getCapacity() != null) {
            if (request.getCapacity() < section.getEnrolledCount()) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể sửa Sĩ số tối đa nhỏ hơn Số sinh viên đã đăng ký (" + section.getEnrolledCount() + ")");
            }
            section.setCapacity(request.getCapacity());
        }

        if (request.getMinStudents() != null) {
            section.setMinStudents(request.getMinStudents());
        }

        return classSectionMapper.toResponse(classSectionRepository.save(section));
    }

    @Override
    @Transactional
    public void deleteClassSection(UUID id) {
        ClassSection section = classSectionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND, "Không tìm thấy lớp học phần"));

        if (section.getEnrolledCount() > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể xóa lớp đã có sinh viên đăng ký. Vui lòng hủy đăng ký hoặc đóng lớp.");
        }

        if (classSectionRepository.existsByParentSectionId(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bạn đang cố xóa lớp Lý thuyết có chứa lớp Thực hành con. Vui lòng xóa lớp Thực hành trước.");
        }

        classSectionRepository.delete(section);
    }

    @Override
    public ClassSectionResponse getClassSectionById(UUID id) {
        ClassSection section = classSectionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND, "Không tìm thấy lớp học phần"));
        return classSectionMapper.toResponse(section);
    }

    @Override
    public List<ClassSectionResponse> getClassSectionsBySubjectIdAndSemesterId(UUID subjectId) {
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND, "Không tìm thấy học kỳ hiện tại"));
        List<com.example.datn.DTO.Response.ClassSectionResponse> allResponses = classSectionRepository.findBySubjectIdAndSemesterId(subjectId, currentSemester.getId()).stream()
                .map(classSectionMapper::toResponse)
                .collect(Collectors.toList());

        java.util.Map<UUID, List<com.example.datn.DTO.Response.ClassSectionResponse>> childrenMap = allResponses.stream()
                .filter(r -> r.getParentSectionId() != null)
                .collect(Collectors.groupingBy(com.example.datn.DTO.Response.ClassSectionResponse::getParentSectionId));

        List<com.example.datn.DTO.Response.ClassSectionResponse> rootSections = new java.util.ArrayList<>();
        for (com.example.datn.DTO.Response.ClassSectionResponse response : allResponses) {
            response.setChildren(childrenMap.getOrDefault(response.getId(), new java.util.ArrayList<>()));
            if (response.getParentSectionId() == null) {
                rootSections.add(response);
            }
        }

        return rootSections;
    }

    @Override
    public Page<SubjectResponse> getOpenedSubjectsPage(UUID semesterId, String keyword, org.springframework.data.domain.Pageable pageable) {
        String safeKeyword = (keyword == null) ? "" : keyword;
        return classSectionRepository.searchOpenedSubjects(semesterId, safeKeyword, pageable)
                .map(subjectMapper::toResponse);
    }



    @Override
    @Transactional
    public void approveClassSection(UUID id) {
        ClassSection section = classSectionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND, "Không tìm thấy lớp học phần"));

        if (section.getStatus() == com.example.datn.ENUM.SectionStatus.OPENED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp học phần này đã được mở.");
        }
        if (section.getStatus() == com.example.datn.ENUM.SectionStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể mở lại lớp học phần đã bị Hủy.");
        }

        section.setStatus(com.example.datn.ENUM.SectionStatus.OPENED);
        classSectionRepository.save(section);
    }

    @Override
    @Transactional
    public void cancelClassSection(UUID id) {
        ClassSection section = classSectionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND, "Không tìm thấy lớp học phần"));

        if (section.getStatus() == com.example.datn.ENUM.SectionStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lớp này đã bị hủy từ trước.");
        }

        if (section.getEnrolledCount() > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể Hủy lớp học phần đang có sinh viên đăng ký. Vui lòng làm rỗng lớp trước khi hủy (Hệ thống chưa hỗ trợ Hủy tự động hoàn tiền).");
        }

        if (classSectionRepository.existsByParentSectionId(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể Hủy lớp Lý thuyết đang có chứa các lớp Thực hành con. Vui lòng hủy lớp thực hành trước.");
        }

        section.setStatus(com.example.datn.ENUM.SectionStatus.CANCELLED);
        classSectionRepository.save(section);
    }

    @Override
    @Transactional
    public void closeClassSection(UUID id) {
        ClassSection section = classSectionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND, "Không tìm thấy lớp học phần"));

        if (section.getStatus() != com.example.datn.ENUM.SectionStatus.OPENED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chỉ có thể Tạm Đóng các lớp học đang ở trạng thái OPENED (Đã Mở).");
        }

        section.setStatus(com.example.datn.ENUM.SectionStatus.CLOSED);
        classSectionRepository.save(section);
    }

    @Override
    public List<ClassSection> getAllClassSectionsBySemesterId(UUID semesterId) {
        return  classSectionRepository.findBySemesterId(semesterId);
    }


}