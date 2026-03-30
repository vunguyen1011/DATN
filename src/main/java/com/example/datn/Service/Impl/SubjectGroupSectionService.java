package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SubjectGroupSectionRequest;
import com.example.datn.DTO.Response.SubjectGroupSectionResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SubjectGroupSectionMapper;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.ISubjectGroupSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectGroupSectionService implements ISubjectGroupSectionService {

    private final SubjectGroupSectionRepository sectionRepository;



    private final EducationProgramRepository programRepository; // Thêm repo CTĐT
    private final SubjectGroupRepository subjectGroupRepository; // Thêm repo Khối Hồng gốc
    private final SubjectGroupSectionMapper sectionMapper;
    private final ProgramSubjectRepository programSubjectRepository;

    @Override
    @Transactional
    public SubjectGroupSectionResponse createSection(SubjectGroupSectionRequest request) {
        // Kiểm tra CTĐT có tồn tại không
        EducationProgram program = programRepository.findByIdAndIsActiveTrue(request.getEducationProgramId())
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));

        // Kiểm tra Khối Hồng gốc có tồn tại không
        SubjectGroup group = subjectGroupRepository.findByIdAndIsActiveTrue(request.getSubjectGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_GROUP_NOT_FOUND));

        // Validate số tín chỉ cho khối tự chọn
        if (!request.getIsMandatory() && (request.getRequiredCredits() == null || request.getRequiredCredits() <= 0)) {
            throw new AppException(ErrorCode.REQUIRED_CREDITS_MISSING);
        }

        // Tạo Entity và gán các quan hệ trực tiếp
        SubjectGroupSection newSection = sectionMapper.toEntity(request);
        newSection.setEducationProgram(program);
        newSection.setSubjectGroup(group);

        SubjectGroupSection saved = sectionRepository.save(newSection);
        SubjectGroupSectionResponse response = sectionMapper.toResponse(saved);
        response.setTotalCredits(0);
        return response;
    }

    @Override
    @Transactional
    public SubjectGroupSectionResponse updateSection(UUID id, SubjectGroupSectionRequest request) {
        SubjectGroupSection existingSection = sectionRepository.findById(id)
                .filter(SubjectGroupSection::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND));

        // Logic validate tín chỉ
        if (!request.getIsMandatory() && (request.getRequiredCredits() == null || request.getRequiredCredits() <= 0)) {
            throw new AppException(ErrorCode.REQUIRED_CREDITS_MISSING);
        }

        sectionMapper.updateEntity(existingSection, request);

        // Nếu thay đổi CTĐT hoặc Nhóm (hiếm khi xảy ra nhưng vẫn nên xử lý)
        if (!existingSection.getEducationProgram().getId().equals(request.getEducationProgramId())) {
            EducationProgram program = programRepository.findByIdAndIsActiveTrue(request.getEducationProgramId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));
            existingSection.setEducationProgram(program);
        }

        SubjectGroupSectionResponse response = sectionMapper.toResponse(sectionRepository.save(existingSection));
        response.setTotalCredits(calculateTotalCredits(id));
        return response;
    }

    @Override
    public SubjectGroupSectionResponse getSectionById(UUID id) {
        SubjectGroupSection section = sectionRepository.findById(id)
                .filter(SubjectGroupSection::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND));

        SubjectGroupSectionResponse response = sectionMapper.toResponse(section);
        response.setTotalCredits(calculateTotalCredits(id));
        return response;
    }

    @Override
    public List<SubjectGroupSectionResponse> getSectionsByProgramId(UUID programId) {
        if (!programRepository.existsById(programId)) {
            throw new AppException(ErrorCode.PROGRAM_NOT_FOUND);
        }

        List<SubjectGroupSection> sections = sectionRepository.findByEducationProgramIdAndIsActiveTrue(programId);

        return sections.stream().map(section -> {
            SubjectGroupSectionResponse response = sectionMapper.toResponse(section);
            response.setTotalCredits(calculateTotalCredits(section.getId()));
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void softDeleteSection(UUID id) {
        SubjectGroupSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND));

        // Không cho xóa nếu khối đã có môn học thực tế
        if (calculateTotalCredits(id) > 0) {
            throw new AppException(ErrorCode.SECTION_CONTAINS_SUBJECTS);
        }

        section.setIsActive(false);
        sectionRepository.save(section);
    }

    // Hàm tính tổng tín chỉ hiện có trong khối (không lưu DB, tính lúc query)
    private Integer calculateTotalCredits(UUID sectionId) {
        return programSubjectRepository.sumCreditsBySectionId(sectionId);
    }
}