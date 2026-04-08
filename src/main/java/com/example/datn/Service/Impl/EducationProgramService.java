package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.EducationProgramRequest;
import com.example.datn.DTO.Request.ProgramCohortRequest;
import com.example.datn.DTO.Response.EducationProgramResponse;
import com.example.datn.DTO.Response.ProgramCohortResponse;
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
    private final ProgramCohortRepository programCohortRepository;
    private final CohortRepository cohortRepository;



    @Override
    @Transactional
    public EducationProgramResponse createProgram(EducationProgramRequest request) {
        if (programRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.PROGRAM_CODE_ALREADY_EXISTS);
        }
        Major major = majorRepository.findByIdAndIsActiveTrue(request.getMajorId())
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));
        EducationProgram program = programMapper.toEntity(request, major);
        EducationProgram savedProgram = programRepository.save(program);

        List<SubjectGroup> globalGroups = subjectGroupRepository.findByIsGlobalTrueAndIsActiveTrue();
        if (globalGroups.isEmpty()) return programMapper.toResponse(savedProgram);
        List<UUID> groupIds = globalGroups.stream().map(SubjectGroup::getId).toList();

        List<SectionDefault> allDefaults = sectionDefaultRepository.findBySubjectGroupIdInAndIsActiveTrue(groupIds);
        Map<UUID, List<SectionDefault>> defaultsByGroup = allDefaults.stream()
                .collect(Collectors.groupingBy(sd -> sd.getSubjectGroup().getId()));

        List<UUID> defaultSectionIds = allDefaults.stream().map(SectionDefault::getId).toList();
        List<SectionDefaultSubject> allDefaultSubjects = sectionDefaultSubjectRepository.findBySectionDefaultIdInAndIsActiveTrue(defaultSectionIds);
        Map<UUID, List<SectionDefaultSubject>> subjectsByDefSection = allDefaultSubjects.stream()
                .collect(Collectors.groupingBy(sds -> sds.getSectionDefault().getId()));

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
    @Transactional
    public EducationProgramResponse updateProgram(UUID id, EducationProgramRequest request) {
        EducationProgram program = programRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));
        if(Boolean.TRUE.equals(program.getIsTemplate())){
            throw new AppException(ErrorCode.PROGRAM_IS_LOCKED);
        }

        if (!program.getCode().equals(request.getCode()) && programRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.PROGRAM_CODE_ALREADY_EXISTS);
        }

        Major major = majorRepository.findByIdAndIsActiveTrue(request.getMajorId())
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        program.setCode(request.getCode());
        program.setName(request.getName());
        program.setTotalCredits(request.getTotalCredits());
        program.setDurationYears(request.getDurationYears());
        program.setMajor(major);

        return programMapper.toResponse(programRepository.save(program));
    }

    @Override
    @Transactional
    public void softDeleteProgram(UUID id) {
        EducationProgram program = programRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));

        if (Boolean.TRUE.equals(program.getIsTemplate())) {
            throw new AppException(ErrorCode.PROGRAM_IS_LOCKED);
        }

        List<ProgramCohort> linkedCohorts = programCohortRepository.findByProgramId(id);
        if (!linkedCohorts.isEmpty()) {
            throw new AppException(ErrorCode.PROGRAM_HAS_STUDENTS_CANNOT_DELETE);
        }
        program.setIsActive(false);
        programRepository.save(program);

    }

    @Override
    @Transactional
    public EducationProgramResponse publishProgram(UUID id) {
        EducationProgram program = programRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));

        if (Boolean.TRUE.equals(program.getIsTemplate())) {
            throw new AppException(ErrorCode.PROGRAM_ALREADY_PUBLISHED);
        }
//        if(!programCohortRepository.existsByProgramId(id)){
//            throw new AppException(ErrorCode.PROGRAM_HAS_NO_COHORTS_CANNOT_PUBLISH);
//        }

        program.setIsTemplate(true);
        programRepository.save(program);
        return programMapper.toResponse(program);
    }

    @Override
    @Transactional(readOnly = true)
    public EducationProgramResponse getProgramById(UUID id) {
        EducationProgram program = programRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));
        return programMapper.toResponse(program);
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



    @Override
    @Transactional(readOnly = true)
    public ProgramTreeResponse getProgramTree(UUID programId) {
        EducationProgram program = programRepository.findByIdAndIsActiveTrue(programId)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));
        List<SubjectGroupSection> sections = sectionRepository.findAllByEducationProgramIdFetchGroup(programId);
        List<ProgramSubject> allSubjects = programSubjectRepository.findAllByProgramIdFetchSubject(programId);
        Map<UUID, List<ProgramSubject>> subjectsBySection = allSubjects.stream()
                .collect(Collectors.groupingBy(ps -> ps.getSection().getId()));
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
    @Transactional
    public ProgramCohortResponse assignProgramToCohort(ProgramCohortRequest request) {
        EducationProgram program = programRepository.findByIdAndIsActiveTrue(request.getProgramId())
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_NOT_FOUND));
        if(!program.getIsTemplate()){
            throw new AppException(ErrorCode.PROGRAM_NOT_PUBLISHED_CANNOT_ASSIGN);
        }

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));

        if (programCohortRepository.existsByProgramIdAndCohortId(request.getProgramId(), request.getCohortId())) {
            throw new AppException(ErrorCode.PROGRAM_COHORT_ALREADY_EXISTS);
        }

        ProgramCohort programCohort = new ProgramCohort();
        programCohort.setProgram(program);
        programCohort.setCohort(cohort);
        programCohort.setAppliedDate(request.getAppliedDate());
        programCohort.setIsActiveForCohort(true);

        ProgramCohort saved = programCohortRepository.save(programCohort);
        return mapToProgramCohortResponse(saved);
    }

    @Override
    @Transactional
    public void removeProgramFromCohort(UUID programId, UUID cohortId) {
        ProgramCohort programCohort = programCohortRepository.findByProgramIdAndCohortId(programId, cohortId)
                .orElseThrow(() -> new AppException(ErrorCode.PROGRAM_COHORT_NOT_FOUND));
        programCohortRepository.delete(programCohort);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramCohortResponse> getCohortsByProgram(UUID programId) {
        if (!programRepository.existsById(programId)) {
            throw new AppException(ErrorCode.PROGRAM_NOT_FOUND);
        }
        return programCohortRepository.findByProgramIdFetchCohort(programId).stream()
                .map(this::mapToProgramCohortResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramCohortResponse> getProgramsByCohort(UUID cohortId) {
        if (!cohortRepository.existsById(cohortId)) {
            throw new AppException(ErrorCode.COHORT_NOT_FOUND);
        }
        return programCohortRepository.findByCohortIdFetchProgram(cohortId).stream()
                .map(this::mapToProgramCohortResponse)
                .collect(Collectors.toList());
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

    private ProgramCohortResponse mapToProgramCohortResponse(ProgramCohort pc) {
        return ProgramCohortResponse.builder()
                .id(pc.getId())
                .programId(pc.getProgram() != null ? pc.getProgram().getId() : null)
                .programCode(pc.getProgram() != null ? pc.getProgram().getCode() : null)
                .programName(pc.getProgram() != null ? pc.getProgram().getName() : null)
                .cohortId(pc.getCohort() != null ? pc.getCohort().getId() : null)
                .cohortName(pc.getCohort() != null ? pc.getCohort().getName() : null)
                .cohortStartYear(pc.getCohort() != null ? pc.getCohort().getStartYear() : null)
                .appliedDate(pc.getAppliedDate())
                .isActiveForCohort(pc.getIsActiveForCohort())
                .build();
    }
}