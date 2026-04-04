package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SectionDefaultSubjectRequest;
import com.example.datn.DTO.Response.SectionDefaultSubjectResponse;
import com.example.datn.DTO.Response.TemplateTreeResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SectionDefaultSubjectMapper;
import com.example.datn.Model.SectionDefault;
import com.example.datn.Model.SectionDefaultSubject;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectGroup;
import com.example.datn.Repository.SectionDefaultRepository;
import com.example.datn.Repository.SectionDefaultSubjectRepository;
import com.example.datn.Repository.SubjectGroupRepository;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Service.Interface.ISectionDefaultSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionDefaultSubjectService implements ISectionDefaultSubjectService {

    // CHÚ Ý: Phải gọi đúng tên biến giống như lúc mình tự khai báo
    private final SectionDefaultSubjectRepository repository; // <--- Biến này thay cho sectionDefaultSubjectRepository
    private final SectionDefaultRepository sectionDefaultRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectGroupRepository subjectGroupRepository; // <--- KHAI BÁO THÊM BEAN NÀY
    private final SectionDefaultSubjectMapper mapper;

    @Override
    @Transactional
    public SectionDefaultSubjectResponse create(SectionDefaultSubjectRequest request) {
        // ... (Giữ nguyên logic của bạn)
        SectionDefault sectionDefault = sectionDefaultRepository.findById(request.getSectionDefaultId())
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .filter(Subject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (repository.existsBySectionDefaultIdAndSubjectIdAndIsActiveTrue(request.getSectionDefaultId(), request.getSubjectId())) {
            throw new AppException(ErrorCode.SUBJECT_ALREADY_EXISTS_IN_SECTION);
        }

        if (Boolean.TRUE.equals(sectionDefault.getIsMandatory())) {
            int currentCredits = sectionDefault.getRequiredCredits() != null ? sectionDefault.getRequiredCredits() : 0;
            sectionDefault.setRequiredCredits(currentCredits + subject.getCredits());
        }

        SectionDefaultSubject entity = mapper.toEntity(request, sectionDefault, subject);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public SectionDefaultSubjectResponse update(UUID id, SectionDefaultSubjectRequest request) {
        // ... (Giữ nguyên logic của bạn)
        SectionDefaultSubject entity = repository.findById(id)
                .filter(SectionDefaultSubject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_SUBJECT_NOT_FOUND));

        SectionDefault sectionDefault = entity.getSectionDefault();
        Subject oldSubject = entity.getSubject();

        Subject newSubject = subjectRepository.findById(request.getSubjectId())
                .filter(Subject::getIsActive)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (!oldSubject.getId().equals(newSubject.getId()) &&
                repository.existsBySectionDefaultIdAndSubjectIdAndIsActiveTrue(sectionDefault.getId(), newSubject.getId())) {
            throw new AppException(ErrorCode.SUBJECT_ALREADY_EXISTS_IN_SECTION);
        }

        if (Boolean.TRUE.equals(sectionDefault.getIsMandatory()) && !oldSubject.getId().equals(newSubject.getId())) {
            int currentCredits = sectionDefault.getRequiredCredits() != null ? sectionDefault.getRequiredCredits() : 0;
            int updatedCredits = currentCredits - oldSubject.getCredits() + newSubject.getCredits();
            sectionDefault.setRequiredCredits(Math.max(0, updatedCredits));
        }

        entity.setSubject(newSubject);
        entity.setDefaultSemester(request.getDefaultSemester());

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID sectionDefaultId, UUID subjectId) {
        // ... (Giữ nguyên logic của bạn)
        SectionDefaultSubject entity = repository.findBySectionDefaultIdAndSubjectIdAndIsActiveTrue(sectionDefaultId, subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SECTION_DEFAULT_SUBJECT_NOT_FOUND));

        SectionDefault sectionDefault = entity.getSectionDefault();
        Subject subject = entity.getSubject();

        if (Boolean.TRUE.equals(sectionDefault.getIsMandatory())) {
            int currentCredits = sectionDefault.getRequiredCredits() != null ? sectionDefault.getRequiredCredits() : 0;
            int updatedCredits = Math.max(0, currentCredits - subject.getCredits());
            sectionDefault.setRequiredCredits(updatedCredits);
        }

        entity.setIsActive(false);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionDefaultSubjectResponse> getBySectionDefaultId(UUID sectionDefaultId) {
        if (!sectionDefaultRepository.existsById(sectionDefaultId)) {
            throw new AppException(ErrorCode.SECTION_DEFAULT_NOT_FOUND);
        }

        return repository.findBySectionDefaultIdAndIsActiveTrue(sectionDefaultId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // Đã thêm @Override nếu bạn khai báo nó trong Interface
    @Override
    public TemplateTreeResponse getTemplateTree() {
        List<SubjectGroup> globalGroups = subjectGroupRepository.findByIsGlobalTrueAndIsActiveTrue();
        if (globalGroups.isEmpty()) {
            return TemplateTreeResponse.builder().groups(Collections.emptyList()).build();
        }

        List<UUID> groupIds = globalGroups.stream().map(SubjectGroup::getId).toList();

        List<SectionDefault> allDefaults = sectionDefaultRepository.findBySubjectGroupIdInAndIsActiveTrue(groupIds);
        Map<UUID, List<SectionDefault>> defaultsByGroup = allDefaults.stream()
                .collect(Collectors.groupingBy(sd -> sd.getSubjectGroup().getId()));

        List<UUID> sectionIds = allDefaults.stream().map(SectionDefault::getId).toList();

        // CHÚ Ý: Sửa lại tên biến repository thành tên mà lombok đang quản lý (repository)
        List<SectionDefaultSubject> allSubjects = repository.findBySectionDefaultIdInAndIsActiveTrue(sectionIds);
        Map<UUID, List<SectionDefaultSubject>> subjectsBySection = allSubjects.stream()
                .collect(Collectors.groupingBy(sds -> sds.getSectionDefault().getId()));

        List<TemplateTreeResponse.GroupNode> groupNodes = globalGroups.stream()
                .sorted(Comparator.comparing(SubjectGroup::getIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(group -> {
                    List<SectionDefault> sections = defaultsByGroup.getOrDefault(group.getId(), Collections.emptyList());

                    List<TemplateTreeResponse.SectionNode> sectionNodes = sections.stream()
                            .sorted(Comparator.comparing(SectionDefault::getIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(section -> {
                                List<SectionDefaultSubject> subjects = subjectsBySection.getOrDefault(section.getId(), Collections.emptyList());

                                List<TemplateTreeResponse.SubjectNode> subjectNodes = subjects.stream()
                                        .map(sub -> TemplateTreeResponse.SubjectNode.builder()
                                                .id(sub.getId())
                                                .subjectId(sub.getSubject().getId())
                                                .subjectName(sub.getSubject().getName())
                                                .subjectCode(sub.getSubject().getCode())
                                                .credits(sub.getSubject().getCredits())
                                                .defaultSemester(sub.getDefaultSemester())
                                                .build())
                                        .toList();

                                return TemplateTreeResponse.SectionNode.builder()
                                        .id(section.getId())
                                        .title(section.getTitle())
                                        .isMandatory(section.getIsMandatory())
                                        .requiredCredits(section.getRequiredCredits())
                                        .index(section.getIndex())
                                        .subjects(subjectNodes)
                                        .build();
                            }).toList();

                    return TemplateTreeResponse.GroupNode.builder()
                            .id(group.getId())
                            .name(group.getName())
                            .index(group.getIndex())
                            .sections(sectionNodes)
                            .build();
                }).toList();

        return TemplateTreeResponse.builder().groups(groupNodes).build();
    }
}