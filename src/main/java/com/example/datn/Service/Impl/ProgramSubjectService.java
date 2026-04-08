package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.ProgramSubjectRequest;
import com.example.datn.DTO.Response.ProgramSubjectResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.ProgramSubjectMapper;
import com.example.datn.Model.ProgramSubject;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectGroupSection;
import com.example.datn.Model.EducationProgram;
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

    private void checkProgramNotLocked(SubjectGroupSection section) {
        EducationProgram program = section.getEducationProgram();
        if (program != null && Boolean.TRUE.equals(program.getIsTemplate())) {
            throw new AppException(ErrorCode.PROGRAM_IS_LOCKED);
        }
    }

    @Override
    @Transactional
    public ProgramSubjectResponse create(ProgramSubjectRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        SubjectGroupSection section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND));

        checkProgramNotLocked(section);

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

        SubjectGroupSection oldSection = existingPs.getSection();
        checkProgramNotLocked(oldSection);

        SubjectGroupSection newSection = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_NOT_FOUND));

        if (!oldSection.getId().equals(newSection.getId())) {
            checkProgramNotLocked(newSection);
        }

        Subject oldSubject = existingPs.getSubject();
        Subject newSubject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        boolean isSubjectChanged = !oldSubject.getId().equals(newSubject.getId());
        boolean isSectionChanged = !oldSection.getId().equals(newSection.getId());

        if ((isSubjectChanged || isSectionChanged) &&
                psRepository.existsBySubjectIdAndSectionIdAndIsActiveTrue(newSubject.getId(), newSection.getId())) {
            throw new AppException(ErrorCode.SUBJECT_ALREADY_EXISTS_IN_SECTION);
        }

        if (isSubjectChanged || isSectionChanged) {
            if (Boolean.TRUE.equals(oldSection.getIsMandatory())) {
                int oldCredits = oldSection.getRequiredCredits() != null ? oldSection.getRequiredCredits() : 0;
                oldSection.setRequiredCredits(Math.max(0, oldCredits - oldSubject.getCredits()));
            }

            if (Boolean.TRUE.equals(newSection.getIsMandatory())) {
                int newCredits = newSection.getRequiredCredits() != null ? newSection.getRequiredCredits() : 0;
                newSection.setRequiredCredits(newCredits + newSubject.getCredits());
            }
        }

        psMapper.updateEntity(existingPs, request, newSubject, newSection);

        return psMapper.toResponse(psRepository.save(existingPs));
    }

    @Override
    public ProgramSubjectResponse getById(UUID id) {
        ProgramSubject ps = psRepository.findById(id)
                .filter(ProgramSubject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_SUBJECT_NOT_FOUND));
        return psMapper.toResponse(ps);
    }

    @Override
    public List<ProgramSubjectResponse> getBySectionId(UUID sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new AppException(ErrorCode.SECTION_NOT_FOUND);
        }
        return psMapper.toResponseList(psRepository.findBySectionIdAndIsActiveTrue(sectionId));
    }

    @Override
    @Transactional
    public void delete(UUID sectionId, UUID subjectId) {
        ProgramSubject ps = psRepository.findBySectionIdAndSubjectIdAndIsActiveTrue(sectionId, subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_SUBJECT_NOT_FOUND));

        SubjectGroupSection section = ps.getSection();
        checkProgramNotLocked(section);

        Subject subject = ps.getSubject();
        if (Boolean.TRUE.equals(section.getIsMandatory())) {
            int currentCredits = section.getRequiredCredits() != null ? section.getRequiredCredits() : 0;
            int updatedCredits = currentCredits - subject.getCredits();
            section.setRequiredCredits(Math.max(0, updatedCredits));
        }
        ps.setIsActive(false);
        psRepository.save(ps);
    }
}