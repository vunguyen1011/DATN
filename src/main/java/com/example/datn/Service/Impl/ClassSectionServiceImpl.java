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
import com.example.datn.ENUM.SectionStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassSectionServiceImpl implements IClassSectionService {

    @Autowired
    private ClassSectionRepository classSectionRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubjectComponentRepository subjectComponentRepository;

    @Override
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Import-Class-Section");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"STT", "Mã Môn", "Tổng Sinh Viên", "Max Lý Thuyết", "Max Thực Hành"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            cell.setCellStyle(headerStyle);
        }

        // Example pre-filled row for user
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(1);
        row.createCell(1).setCellValue("ITD001");
        row.createCell(2).setCellValue(100);
        row.createCell(3).setCellValue(100);
        row.createCell(4).setCellValue(40);

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"Template_Mo_Lop.xlsx\"");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Override
    @Transactional
    public String importClassSections(UUID semesterId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FORMAT);
        }

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

//        if (semester.getCode() == null || semester.getCode().isEmpty()) {
//            throw new RuntimeException("Semester code is required for generating section code. Vui lòng cập nhật mã cho học kỳ.");
//        }

        List<ClassSection> sectionsToSave = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Bỏ qua header
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Lấy thông tin từ file excel
                Cell subjectCodeCell = row.getCell(1);
                Cell totalStudentsCell = row.getCell(2);
                Cell maxLTCell = row.getCell(3);
                Cell maxTHCell = row.getCell(4);

                if (subjectCodeCell == null || subjectCodeCell.getCellType() == CellType.BLANK) {
                    continue;
                }

                String subjectCode = subjectCodeCell.getStringCellValue().trim();
                int totalStudents = (totalStudentsCell != null && totalStudentsCell.getCellType() == CellType.NUMERIC) ? (int) totalStudentsCell.getNumericCellValue() : 0;
                int maxLT = (maxLTCell != null && maxLTCell.getCellType() == CellType.NUMERIC) ? (int) maxLTCell.getNumericCellValue() : 0;
                int maxTH = (maxTHCell != null && maxTHCell.getCellType() == CellType.NUMERIC) ? (int) maxTHCell.getNumericCellValue() : 0;

                if (totalStudents <= 0) continue;

                // 1. Tìm môn học và các component
                Optional<Subject> subjectOpt = subjectRepository.findByCode(subjectCode);
                if (subjectOpt.isEmpty()) {
                    continue; // Có thể log lại môn bị lỗi thay vì throw exception để import mượt hơn
                }
                Subject subject = subjectOpt.get();
                List<SubjectComponent> components = subjectComponentRepository.findBySubjectId(subject.getId());
                if (components.isEmpty()) continue;

                // 2. Tách lớp Lõi (Primary) và lớp Vệ tinh (Children)
                SubjectComponent primaryComp = null;
                List<SubjectComponent> childComps = new ArrayList<>();

                Optional<SubjectComponent> theoryOpt = components.stream().filter(c -> c.getType() == ComponentType.THEORY).findFirst();
                if (theoryOpt.isPresent()) {
                    primaryComp = theoryOpt.get();
                    childComps = components.stream().filter(c -> c.getType() != ComponentType.THEORY).collect(Collectors.toList());
                } else {
                    primaryComp = components.get(0);
                    for (int i = 1; i < components.size(); i++) {
                        childComps.add(components.get(i));
                    }
                }

                // 3. Quy định dung lượng cho Primary dựa theo đúng Thể loại
                int defaultPrimaryMax = 100;
                int excelPrimaryMax = maxLT;
                if (primaryComp.getType() == ComponentType.PRACTICE) {
                    defaultPrimaryMax = 40;
                    excelPrimaryMax = maxTH;
                } else if (primaryComp.getType() != ComponentType.THEORY) {
                    defaultPrimaryMax = 40; // Fallback cho PROJECT...
                }

                int remainingStudents = totalStudents;
                int currentSectionIndex = classSectionRepository.countBySemesterIdAndSubjectComponent_SubjectIdAndParentSectionIsNull(semesterId, subject.getId());

                while (remainingStudents > 0) {
                    int finalMaxPrimary = (excelPrimaryMax > 0) ? excelPrimaryMax : defaultPrimaryMax;

                    // Lớp Bố hiện tại chứa được max là finalMaxPrimary, nhưng nếu Sinh viên dư ít hơn thì chỉ lấy phần dư.
                    int studentsForThisSection = Math.min(remainingStudents, finalMaxPrimary);

                    currentSectionIndex++;
                    String parentCodeSuffix = String.format("%02d", currentSectionIndex);
                    String parentSectionCode = subjectCode + "_" + semester.getCode() + "_" + parentCodeSuffix;

                    ClassSection parentSection = ClassSection.builder()
                            .sectionCode(parentSectionCode)
                            .courseGroupCode(parentCodeSuffix)
                            .subjectComponent(primaryComp)
                            .semester(semester)
                            .capacity(finalMaxPrimary)
                            .minStudents(15) // Fallback hardcode
                            .build();

                    sectionsToSave.add(parentSection);

                    // Mở các lớp Vệ Tinh (Thực hành, Bài tập) để gồng lượng sinh viên của Lớp Bố
                    for (SubjectComponent childComp : childComps) {
                        int finalMaxChild = (maxTH > 0) ? maxTH : 40;
                        int numChildSections = (int) Math.ceil((double) studentsForThisSection / finalMaxChild);

                        for (int j = 1; j <= numChildSections; j++) {
                            String childSectionCode = parentSectionCode + "." + j;

                            ClassSection childSection = ClassSection.builder()
                                    .sectionCode(childSectionCode)
                                    .courseGroupCode(parentCodeSuffix)
                                    .subjectComponent(childComp)
                                    .semester(semester)
                                    .parentSection(parentSection) // Map CHA - CON
                                    .capacity(finalMaxChild)
                                    .minStudents(15)
                                    .build();

                            sectionsToSave.add(childSection);
                        }
                    }

                    remainingStudents -= studentsForThisSection;
                }
            }

            // Thực thi INSERT Batched 1 lần duy nhất cho toàn bộ danh sách để O(1) Transactional DB
            classSectionRepository.saveAll(sectionsToSave);

        } catch (Exception e) {
            e.printStackTrace();
            throw new AppException(ErrorCode.EXCEL_READ_ERROR);
        }

        return "Successfully created " + sectionsToSave.size() + " class sections.";
    }
}
