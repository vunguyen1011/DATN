package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.ProgramSubjectRequest;
import com.example.datn.DTO.Response.ProgramSubjectResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.ProgramSubjectMapper;
import com.example.datn.Model.ProgramSubject;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectGroupSection;
import com.example.datn.Repository.ProgramSubjectRepository;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Repository.SubjectGroupSectionRepository;
import com.example.datn.Service.Interface.IProgramSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramSubjectService implements IProgramSubjectService {

    private final ProgramSubjectRepository psRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectGroupSectionRepository sectionRepository;
    private final ProgramSubjectMapper psMapper;

    @Override
    @Transactional
    public ProgramSubjectResponse create(ProgramSubjectRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        SubjectGroupSection section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND));

        if (psRepository.existsBySubjectIdAndSectionIdAndIsActiveTrue(request.getSubjectId(), request.getSectionId())) {
            throw new AppException(ErrorCode.SUBJECT_ALREADY_EXISTS_IN_SECTION);
        }

        if (Boolean.TRUE.equals(section.getIsMandatory())) {
            int currentCredits = section.getRequiredCredits() != null ? section.getRequiredCredits() : 0;
            section.setRequiredCredits(currentCredits + subject.getCredits());
        }

        ProgramSubject ps = psMapper.toEntity(request, subject, section);
        return psMapper.toResponse(psRepository.save(ps));
    }

    @Override
    @Transactional
    public ProgramSubjectResponse update(UUID id, ProgramSubjectRequest request) {
        ProgramSubject existingPs = psRepository.findById(id)
                .filter(ProgramSubject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_SUBJECT_NOT_FOUND));

        Subject newSubject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        SubjectGroupSection section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND));

        Subject oldSubject = existingPs.getSubject();

        if (!oldSubject.getId().equals(newSubject.getId()) &&
                psRepository.existsBySubjectIdAndSectionIdAndIsActiveTrue(newSubject.getId(), section.getId())) {
            throw new AppException(ErrorCode.SUBJECT_ALREADY_EXISTS_IN_SECTION);
        }

        if (Boolean.TRUE.equals(section.getIsMandatory()) && !oldSubject.getId().equals(newSubject.getId())) {
            int currentCredits = section.getRequiredCredits() != null ? section.getRequiredCredits() : 0;
            int updatedCredits = currentCredits - oldSubject.getCredits() + newSubject.getCredits();
            section.setRequiredCredits(Math.max(0, updatedCredits));
        }

        // Sử dụng hàm updateEntity trong Mapper để đồng bộ dữ liệu
        psMapper.updateEntity(existingPs, request, newSubject, section);

        return psMapper.toResponse(psRepository.save(existingPs));
    }

    @Override
    public ProgramSubjectResponse getById(UUID id) {
        ProgramSubject ps = psRepository.findById(id)
                .filter(ProgramSubject::getIsActive) //
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_SUBJECT_NOT_FOUND));
        return psMapper.toResponse(ps);
    }

    @Override
    public List<ProgramSubjectResponse> getBySectionId(UUID sectionId) {
        if(!sectionRepository.existsById(sectionId)) {
            throw new AppException(ErrorCode.SECTION_NOT_FOUND);
        }
        return psMapper.toResponseList(psRepository.findBySectionIdAndIsActiveTrue(sectionId));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ProgramSubject ps = psRepository.findById(id)
                .filter(ProgramSubject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_SUBJECT_NOT_FOUND));

        SubjectGroupSection section = ps.getSection();
        Subject subject = ps.getSubject();
        if (Boolean.TRUE.equals(section.getIsMandatory())) {
            int currentCredits = section.getRequiredCredits() != null ? section.getRequiredCredits() : 0;
            int updatedCredits = currentCredits - subject.getCredits();
            section.setRequiredCredits(Math.max(0, updatedCredits));
        }

        // Thực hiện xóa mềm
        ps.setIsActive(false);
        psRepository.save(ps);
    }
}