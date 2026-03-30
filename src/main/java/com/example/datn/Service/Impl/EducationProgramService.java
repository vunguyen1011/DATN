package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EducationProgramRequest;
import com.example.datn.DTO.Response.EducationProgramResponse;
import com.example.datn.DTO.Response.ProgramTreeResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.EducationProgramMapper;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IEducationProgramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EducationProgramService implements IEducationProgramService {

    private final EducationProgramRepository programRepository;
    private final MajorRepository majorRepository;
    private final EducationProgramMapper programMapper;
    private final SubjectGroupSectionRepository sectionRepository;
    private final ProgramSubjectRepository programSubjectRepository;
    private final SubjectGroupRepository subjectGroupRepository;
    private final SectionDefaultRepository sectionDefaultRepository;
    private final SectionDefaultSubjectRepository sectionDefaultSubjectRepository;

    @Override
    @Transactional
    public EducationProgramResponse createProgram(EducationProgramRequest request) {
        if (programRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.PROGRAM_CODE_ALREADY_EXISTS);
        }
        Major major = majorRepository.findByIdAndIsActiveTrue(request.getMajorId())
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));
        // 1. Lưu CTĐT gốc
        EducationProgram program = programMapper.toEntity(request, major);
        EducationProgram savedProgram = programRepository.save(program);
        // 2. TỐI ƯU: Lấy toàn bộ Khối Hồng Global
        List<SubjectGroup> globalGroups = subjectGroupRepository.findByIsGlobalTrueAndIsActiveTrue();
        if (globalGroups.isEmpty()) return programMapper.toResponse(savedProgram);
        List<UUID> groupIds = globalGroups.stream().map(SubjectGroup::getId).toList();
        // 3. TỐI ƯU: Lấy toàn bộ Khối Cam mẫu của tất cả Khối Hồng trong 1 câu SELECT
        List<SectionDefault> allDefaults = sectionDefaultRepository.findBySubjectGroupIdInAndIsActiveTrue(groupIds);
        Map<UUID, List<SectionDefault>> defaultsByGroup = allDefaults.stream()
                .collect(Collectors.groupingBy(sd -> sd.getSubjectGroup().getId()));
        // 4. TỐI ƯU: Lấy toàn bộ Môn học mẫu trong 1 câu SELECT
        List<UUID> defaultSectionIds = allDefaults.stream().map(SectionDefault::getId).toList();
        List<SectionDefaultSubject> allDefaultSubjects = sectionDefaultSubjectRepository.findBySectionDefaultIdInAndIsActiveTrue(defaultSectionIds);
        Map<UUID, List<SectionDefaultSubject>> subjectsByDefSection = allDefaultSubjects.stream()
                .collect(Collectors.groupingBy(sds -> sds.getSectionDefault().getId()));
        // 5. Thực hiện lưu dữ liệu thực tế
        for (SubjectGroup group : globalGroups) {
            List<SectionDefault> defaults = defaultsByGroup.getOrDefault(group.getId(), Collections.emptyList());
            for (SectionDefault sd : defaults) {
                SubjectGroupSection section = sectionRepository.save(SubjectGroupSection.builder()
                        .educationProgram(savedProgram)
                        .subjectGroup(group)
                        .title(sd.getTitle())
                        .requiredCredits(sd.getRequiredCredits())
                        .isMandatory(sd.getIsMandatory())
                        .index(sd.getIndex())
                        .isActive(true)
                        .build());
                List<SectionDefaultSubject> defSubjects = subjectsByDefSection.getOrDefault(sd.getId(), Collections.emptyList());
                if (!defSubjects.isEmpty()) {
                    List<ProgramSubject> programSubjects = defSubjects.stream()
                            .map(ds -> ProgramSubject.builder()
                                    .section(section)
                                    .subject(ds.getSubject())
                                    .semester(ds.getDefaultSemester())
                                    .isActive(true)
                                    .build())
                            .toList();
                    programSubjectRepository.saveAll(programSubjects);
                }
            }
        }
        return programMapper.toResponse(savedProgram);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramTreeResponse getProgramTree(UUID programId) {
        EducationProgram program = programRepository.findByIdAndIsActiveTrue(programId)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));
        // 1. TỐI ƯU: Lấy toàn bộ Khối Cam kèm theo Khối Hồng (Join Fetch) để tránh n+1
        List<SubjectGroupSection> sections = sectionRepository.findAllByEducationProgramIdFetchGroup(programId);
        // 2. TỐI ƯU: Lấy toàn bộ môn học kèm theo Subject thông qua 1 câu SELECT duy nhất
        List<ProgramSubject> allSubjects = programSubjectRepository.findAllByProgramIdFetchSubject(programId);
        Map<UUID, List<ProgramSubject>> subjectsBySection = allSubjects.stream()
                .collect(Collectors.groupingBy(ps -> ps.getSection().getId()));
        // 3. Xử lý logic Grouping trên RAM
        Map<SubjectGroup, List<SubjectGroupSection>> groupedSections = sections.stream()
                .filter(sec -> sec.getSubjectGroup() != null)
                .collect(Collectors.groupingBy(SubjectGroupSection::getSubjectGroup));
        List<ProgramTreeResponse.GroupNode> groupNodes = groupedSections.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(SubjectGroup::getIndex)))
                .map(entry -> {
                    SubjectGroup group = entry.getKey();
                    List<ProgramTreeResponse.SectionNode> sectionNodes = entry.getValue().stream()
                            .sorted(Comparator.comparing(SubjectGroupSection::getIndex))
                            .map(section -> {
                                List<ProgramSubject> subjects = subjectsBySection.getOrDefault(section.getId(), Collections.emptyList());
                                return ProgramTreeResponse.SectionNode.builder()
                                        .id(section.getId())
                                        .title(section.getTitle())
                                        .isMandatory(section.getIsMandatory())
                                        .requiredCredits(section.getRequiredCredits())
                                        .subjects(subjects.stream().map(this::mapToSubjectNode).toList())
                                        .build();
                            }).toList();
                    return ProgramTreeResponse.GroupNode.builder()
                            .id(group.getId())
                            .name(group.getName())
                            .index(group.getIndex())
                            .sections(sectionNodes)
                            .build();
                }).toList();
        return ProgramTreeResponse.builder()
                .id(program.getId())
                .programName(program.getName())
                .durationYears(program.getDurationYears())
                .groups(groupNodes)
                .build();
    }


    @Override
    public List<EducationProgramResponse> getAllPrograms(String param) {
        if (param == null || param.trim().isEmpty()) {
            return programRepository.findAllByIsActiveTrue().stream()
                    .map(programMapper::toResponse)
                    .collect(Collectors.toList());
        } else {
            return programRepository.searchByNameOrCode(param).stream()
                    .map(programMapper::toResponse)
                    .collect(Collectors.toList());
        }
    }

    private ProgramTreeResponse.SubjectNode mapToSubjectNode(ProgramSubject ps) {
        var sub = ps.getSubject();
        return ProgramTreeResponse.SubjectNode.builder()
                .id(ps.getId())
                .subjectId(sub.getId())
                .subjectName(sub.getName())
                .subjectCode(sub.getCode())
                .credits(sub.getCredits())
                .build();
    }






}