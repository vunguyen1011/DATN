package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.StudentUpdateRequest;
import com.example.datn.DTO.Response.ProgramSubjectResponse;
import com.example.datn.DTO.Response.ProgramTreeResponse;
import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.StudentMapper;
import com.example.datn.Model.ProgramCohort;
import com.example.datn.Model.Student;
import com.example.datn.Model.User;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IEducationProgramService;
import com.example.datn.Service.Interface.IProgramSubjectService;
import com.example.datn.Service.Interface.IStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class    StudentService implements IStudentService {
    
    private final StudentRepository studentRepository;
    private final CohortRepository cohortRepository;
    private final MajorRepository majorRepository;
    private final AdminClassRepository adminClassRepository;
    private final UserRepository userRepository;
    private final IProgramSubjectService programSubjectService;
    private final ProgramCohortRepository programCohortRepository;
    private final IEducationProgramService educationProgramService;

    @Override
    @Transactional
    public UserProfileResponse.StudentProfile updateStudentProfile(UUID studentId, StudentUpdateRequest request) {
        User user =userRepository.findById(studentId).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        Student student = studentRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)); 
        
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            student.setFullName(request.getFullName().trim());
            if (student.getUser() != null) {
                student.getUser().setFullName(request.getFullName().trim());
            }
        }
        if (request.getPhone() != null) student.setPhone(request.getPhone());
        if (request.getAddress() != null) student.setAddress(request.getAddress());
        if (request.getGender() != null) student.setGender(request.getGender());
        if (request.getStatus() != null) student.setStatus(request.getStatus());

        if (request.getCohortId() != null) {
            student.setCohort(cohortRepository.findById(request.getCohortId())
                    .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND)));
        }
        if (request.getMajorId() != null) {
            student.setMajor(majorRepository.findById(request.getMajorId())
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND)));
        }
        if (request.getAdminClassId() != null) {
            student.setAdminClass(adminClassRepository.findById(request.getAdminClassId())
                    .orElseThrow(() -> new AppException(ErrorCode.ADMIN_CLASS_NOT_FOUND)));
        }
        
        student = studentRepository.save(student);
        
        return StudentMapper.toStudentProfile(student);
    }

    @Override
    public void exportStudentsToPdf(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        java.util.List<Student> students = studentRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "studentCode")
        );

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Danh_sach_Sinh_vien.pdf");

        try (com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate())) {
            com.lowagie.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12);
            com.lowagie.text.Font normalFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 12);

            try {
                com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont("C:\\Windows\\Fonts\\arial.ttf", com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED);
                titleFont = new com.lowagie.text.Font(bf, 18, com.lowagie.text.Font.BOLD);
                headerFont = new com.lowagie.text.Font(bf, 12, com.lowagie.text.Font.BOLD);
                normalFont = new com.lowagie.text.Font(bf, 12, com.lowagie.text.Font.NORMAL);
            } catch (Exception e) {
                // fall back to default if no font found
                 java.util.logging.Logger.getLogger(StudentService.class.getName()).warning("[PDF] Could not load Arial font");
            }

            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("DANH SÁCH SINH VIÊN", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            if (students.isEmpty()) {
                document.add(new com.lowagie.text.Paragraph("Không có sinh viên nào trong hệ thống.", normalFont));
                return;
            }

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3f, 1f, 2f, 1.5f, 2f, 2f});

            String[] headers = {"Mã SV", "Họ Tên", "Giới Tính", "Lớp HC", "Khóa", "Chuyên Ngành", "Trạng Thái"};
            for (String header : headers) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(header, headerFont));
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (Student s : students) {
                table.addCell(new com.lowagie.text.Phrase(s.getStudentCode() != null ? s.getStudentCode() : "", normalFont));
                table.addCell(new com.lowagie.text.Phrase(s.getFullName() != null ? s.getFullName() : "", normalFont));
                
                String gender = "";
                if (s.getGender() != null) {
                    gender = s.getGender().name().equalsIgnoreCase("MALE") ? "Nam" : "Nữ";
                }
                table.addCell(new com.lowagie.text.Phrase(gender, normalFont));
                
                table.addCell(new com.lowagie.text.Phrase(s.getAdminClass() != null ? s.getAdminClass().getName() : "", normalFont));
                table.addCell(new com.lowagie.text.Phrase(s.getCohort() != null ? s.getCohort().getName() : "", normalFont));
                table.addCell(new com.lowagie.text.Phrase(s.getMajor() != null ? s.getMajor().getName() : "", normalFont));
                
                String status = "";
                if (s.getStatus() != null) {
                    status = s.getStatus().name().equals("STUDYING") ? "Đang học" : s.getStatus().name();
                }
                table.addCell(new com.lowagie.text.Phrase(status, normalFont));
            }

            document.add(table);
        }
    }

    @Override
    public List<ProgramSubjectResponse> getMyProgram() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Student student = studentRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (student.getCohort() == null || student.getMajor() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        return programSubjectService.getSubjectsByCohortAndMajor(
                student.getCohort().getId(),
                student.getMajor().getId()
        );
    }

    @Override
    public ProgramTreeResponse getMyProgramTree() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Student student = studentRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (student.getCohort() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // Tìm chương trình đào tạo của khóa học sinh viên đang theo học
        List<ProgramCohort> programCohorts = programCohortRepository.findByCohortIdFetchProgram(
                student.getCohort().getId()
        );

        if (programCohorts.isEmpty()) {
            throw new AppException(ErrorCode.PROGRAM_NOT_FOUND);
        }

        // Lấy chương trình đào tạo đầu tiên (1 khóa chỉ có 1 CTĐT)
        UUID programId = programCohorts.get(0).getProgram().getId();

        return educationProgramService.getProgramTree(programId);
    }
}
