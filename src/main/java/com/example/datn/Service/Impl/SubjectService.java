package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SubjectRequest;
import com.example.datn.DTO.Response.SubjectResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SubjectMapper;
import com.example.datn.Model.Prerequisite;
import com.example.datn.Model.Subject;
import com.example.datn.Repository.PrerequisiteRepository;
import com.example.datn.Repository.ProgramSubjectRepository;
import com.example.datn.Repository.SectionDefaultSubjectRepository;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Service.Interface.ISubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class SubjectService implements ISubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final PrerequisiteRepository prerequisiteRepository;
    private final SectionDefaultSubjectRepository sectionDefaultSubjectRepository;
    private final ProgramSubjectRepository programSubjectRepository;

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Optional<Subject> existingSubjectOpt = subjectRepository.findByCode(request.getCode());

        if (existingSubjectOpt.isPresent()) {
            Subject existingSubject = existingSubjectOpt.get();

            if (existingSubject.getIsActive()) {
                throw new AppException(ErrorCode.SUBJECT_EXISTED);
            }

            // Khôi phục record đã xóa mềm và cập nhật thông tin mới
            subjectMapper.updateEntityFromRequest(existingSubject, request);
            existingSubject.setIsActive(true);
            return subjectMapper.toResponse(subjectRepository.save(existingSubject));
        }

        // Kiểm tra trùng tên (chỉ check những môn đang active)
        if (subjectRepository.existsByNameAndIsActiveTrue(request.getName())) {
            throw new AppException(ErrorCode.SUBJECT_EXISTED);
        }

        Subject subject = subjectMapper.toEntity(request);
        subject.setIsActive(true);
        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(UUID id, SubjectRequest request) {
        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (!subject.getCode().equals(request.getCode())
                && subjectRepository.existsByCodeAndIsActiveTrue(request.getCode())) {
            throw new AppException(ErrorCode.SUBJECT_EXISTED);
        }

        if (!subject.getName().equals(request.getName())
                && subjectRepository.existsByNameAndIsActiveTrue(request.getName())) {
            throw new AppException(ErrorCode.SUBJECT_EXISTED);
        }

        subjectMapper.updateEntityFromRequest(subject, request);
        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Override
    public Page<SubjectResponse> getAllSubjects(String keyword, Pageable pageable) {
        Page<Subject> subjects;
        if (keyword != null && !keyword.trim().isEmpty()) {
            subjects = subjectRepository.searchActiveByCodeOrName(keyword.trim(), pageable);
        } else {
            subjects = subjectRepository.findByIsActiveTrue(pageable);
        }
        return subjects.map(subjectMapper::toResponse);
    }

    @Override
    public SubjectResponse getSubjectById(UUID id) {
        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        return subjectMapper.toResponse(subject);
    }

    @Override
    @Transactional
    public void deleteSubject(UUID id) {
        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        boolean hasActiveDependent = prerequisiteRepository.findByPrerequisiteSubjectId(id)
                .stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getSubject().getIsActive()));

        if (hasActiveDependent) {
            throw new AppException(ErrorCode.SUBJECT_IS_PREREQUISITE_CANNOT_DELETE);
        }

        if (sectionDefaultSubjectRepository.existsBySubjectIdAndIsActiveTrue(id)) {
            throw new AppException(ErrorCode.SUBJECT_IN_USE_IN_TEMPLATE);
        }

        if (programSubjectRepository.existsBySubjectIdAndIsActiveTrue(id)) {
            throw new AppException(ErrorCode.SUBJECT_IN_USE_IN_PROGRAM);
        }

        subject.setIsActive(false);
        subjectRepository.save(subject);
    }

    @Override
    public List<SubjectResponse> getPrerequisites(UUID subjectId) {
        if (!subjectRepository.existsByIdAndIsActiveTrue(subjectId)) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        return prerequisiteRepository.findBySubjectId(subjectId).stream()
                .map(record -> subjectMapper.toResponse(record.getPrerequisiteSubject()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updatePrerequisites(UUID subjectId, List<UUID> prerequisiteIds) {
        Subject subject = subjectRepository.findByIdAndIsActiveTrue(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (prerequisiteIds != null && prerequisiteIds.contains(subjectId)) {
            throw new AppException(ErrorCode.SUBJECT_INVALID_PREREQUISITE);
        }

        prerequisiteRepository.deleteBySubjectId(subjectId);

        if (prerequisiteIds == null || prerequisiteIds.isEmpty()) {
            return;
        }

        List<Subject> prereqSubjects = subjectRepository.findAllByIdInAndIsActiveTrue(prerequisiteIds);
        if (prereqSubjects.size() != prerequisiteIds.size()) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }

        for (UUID pId : prerequisiteIds) {
            if (isCircularDependency(pId, subjectId)) {
                throw new AppException(ErrorCode.CIRCULAR_PREREQUISITE_DETECTED);
            }
        }

        List<Prerequisite> newPrerequisites = prereqSubjects.stream().map(prereq -> Prerequisite.builder()
                .subject(subject)
                .prerequisiteSubject(prereq)
                .build()).collect(Collectors.toList());

        prerequisiteRepository.saveAll(newPrerequisites);
    }

    @Override
    public List<SubjectResponse> getDependentSubjects(UUID subjectId) {
        if (!subjectRepository.existsByIdAndIsActiveTrue(subjectId)) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        return prerequisiteRepository.findByPrerequisiteSubjectId(subjectId).stream()
                .map(record -> subjectMapper.toResponse(record.getSubject()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectResponse> getPrerequisiteTree(UUID subjectId) {
        if (!subjectRepository.existsByIdAndIsActiveTrue(subjectId)) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        Set<Subject> allPrerequisites = new HashSet<>();
        fetchPrerequisitesRecursive(subjectId, allPrerequisites);
        return allPrerequisites.stream()
                .map(subjectMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void fetchPrerequisitesRecursive(UUID currentId, Set<Subject> result) {
        List<Prerequisite> prereqs = prerequisiteRepository.findBySubjectId(currentId);
        for (Prerequisite p : prereqs) {
            Subject reqSubject = p.getPrerequisiteSubject();
            if (!result.contains(reqSubject)) {
                result.add(reqSubject);
                fetchPrerequisitesRecursive(reqSubject.getId(), result);
            }
        }
    }

    private boolean isCircularDependency(UUID newPrerequisiteId, UUID targetSubjectId) {
        if (newPrerequisiteId.equals(targetSubjectId)) {
            return true;
        }
        List<Prerequisite> prereqs = prerequisiteRepository.findBySubjectId(newPrerequisiteId);
        for (Prerequisite p : prereqs) {
            if (isCircularDependency(p.getPrerequisiteSubject().getId(), targetSubjectId)) {
                return true;
            }
        }
        return false;
    }
}