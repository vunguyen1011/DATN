package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SectionDefaultRequest;
import com.example.datn.DTO.Response.SectionDefaultResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SectionDefaultMapper;
import com.example.datn.Model.SectionDefault;
import com.example.datn.Model.SubjectGroup;
import com.example.datn.Repository.SectionDefaultRepository;
import com.example.datn.Repository.SubjectGroupRepository;
import com.example.datn.Service.Interface.ISectionDefaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionDefaultService implements ISectionDefaultService {

    private final SectionDefaultRepository sectionDefaultRepository;
    private final SubjectGroupRepository subjectGroupRepository;
    private final SectionDefaultMapper sectionDefaultMapper;

    @Override
    @Transactional
    public SectionDefaultResponse createSectionDefault(SectionDefaultRequest request) {
        SubjectGroup subjectGroup = subjectGroupRepository.findById(request.getSubjectGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_GROUP_NOT_FOUND));

        if (request.getIsMandatory() && request.getRequiredCredits() != 0) {
            throw new AppException(ErrorCode.REQUIRED_CREDITS_MISSING);
        }
        if (!request.getIsMandatory() && (request.getRequiredCredits() == null || request.getRequiredCredits() == 0)) {
            throw new AppException(ErrorCode.REQUIRED_CREDITS_MISSING);
        }

        if (sectionDefaultRepository.existsByTitleAndSubjectGroupId(request.getTitle(), request.getSubjectGroupId())) {
            throw new AppException(ErrorCode.SECTION_DEFAULT_TITLE_ALREADY_EXISTS);
        }

        SectionDefault sectionDefault = sectionDefaultMapper.toEntity(request, subjectGroup);

        if (!sectionDefault.getIsMandatory()) {
            sectionDefault.setNote("Chọn tối thiểu " + sectionDefault.getRequiredCredits() + " tín chỉ trong khối này");
        } else {
            sectionDefault.setNote(null);
        }

        return sectionDefaultMapper.toResponse(sectionDefaultRepository.save(sectionDefault));
    }

    @Override
    @Transactional
    public SectionDefaultResponse updateSectionDefault(UUID id, SectionDefaultRequest request) {
        SectionDefault sectionDefault = sectionDefaultRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_NOT_FOUND));

        if (request.getSubjectGroupId() != null) {
            SubjectGroup subjectGroup = subjectGroupRepository.findById(request.getSubjectGroupId())
                    .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_GROUP_NOT_FOUND));
            sectionDefault.setSubjectGroup(subjectGroup);
        }

        if (request.getTitle() != null && !request.getTitle().equals(sectionDefault.getTitle())) {
            if (sectionDefaultRepository.existsByTitleAndSubjectGroupId(request.getTitle(), sectionDefault.getSubjectGroup().getId())) {
                throw new AppException(ErrorCode.SECTION_DEFAULT_TITLE_ALREADY_EXISTS);
            }
            sectionDefault.setTitle(request.getTitle());
        }

        if (request.getIsMandatory() != null) {
            sectionDefault.setIsMandatory(request.getIsMandatory());
        }

        if (request.getRequiredCredits() != null) {
            sectionDefault.setRequiredCredits(request.getRequiredCredits());
        }

        if (request.getIndex() != null) {
            sectionDefault.setIndex(request.getIndex());
        }

        if (sectionDefault.getIsMandatory() && sectionDefault.getRequiredCredits() != 0) {
            throw new AppException(ErrorCode.REQUIRED_CREDITS_MISSING);
        }
        if (!sectionDefault.getIsMandatory() && (sectionDefault.getRequiredCredits() == null || sectionDefault.getRequiredCredits() == 0)) {
            throw new AppException(ErrorCode.REQUIRED_CREDITS_MISSING);
        }

        if (!sectionDefault.getIsMandatory()) {
            sectionDefault.setNote("Chọn tối thiểu " + sectionDefault.getRequiredCredits() + " tín chỉ trong khối này");
        } else {
            sectionDefault.setNote(null);
        }

        return sectionDefaultMapper.toResponse(sectionDefaultRepository.save(sectionDefault));
    }

    @Override
    @Transactional
    public void deleteSectionDefault(UUID id) {
        SectionDefault sectionDefault = sectionDefaultRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_NOT_FOUND));

        sectionDefault.setIsActive(false);
        sectionDefaultRepository.save(sectionDefault);
    }

    @Override
    public List<SectionDefaultResponse> getBySubjectGroupId(UUID subjectGroupId) {
        return sectionDefaultRepository.findBySubjectGroupIdOrderByIndexAsc(subjectGroupId)
                .stream()
                .filter(SectionDefault::getIsActive)
                .map(sectionDefaultMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SectionDefaultResponse getById(UUID id) {
        SectionDefault sectionDefault = sectionDefaultRepository.findById(id)
                .filter(SectionDefault::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_NOT_FOUND));
        return sectionDefaultMapper.toResponse(sectionDefault);
    }
}