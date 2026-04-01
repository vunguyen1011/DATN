package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SubjectComponentRequest;
import com.example.datn.DTO.Response.SubjectComponentResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SubjectComponentMapper;
import com.example.datn.Model.SubjectComponent;
import com.example.datn.Repository.SubjectComponentRepository;
import com.example.datn.Service.Interface.ISubjectComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectComponentServiceImpl implements ISubjectComponentService {

    private final SubjectComponentRepository subjectComponentRepository;
    private final SubjectComponentMapper subjectComponentMapper;

    @Override
    @Transactional
    public SubjectComponentResponse createSubjectComponent(SubjectComponentRequest request) {
        // Calculate totalPeriods automatically if not provided or valid
        if (request.getTotalPeriods() == null || request.getTotalPeriods() <= 0) {
            if (request.getSessionsPerWeek() != null && request.getPeriodsPerSession() != null) {
                // Assuming standard 15 weeks if missing. Can be adjusted.
                request.setTotalPeriods(request.getSessionsPerWeek() * request.getPeriodsPerSession() * 15);
            }
        }
        
        SubjectComponent entity = subjectComponentMapper.toEntity(request);
        SubjectComponent savedEntity = subjectComponentRepository.save(entity);
        return subjectComponentMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public SubjectComponentResponse updateSubjectComponent(UUID id, SubjectComponentRequest request) {
        SubjectComponent entity = subjectComponentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND));

        subjectComponentMapper.updateEntityFromRequest(entity, request);
        SubjectComponent updatedEntity = subjectComponentRepository.save(entity);
        return subjectComponentMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectComponentResponse getSubjectComponentById(UUID id) {
        SubjectComponent entity = subjectComponentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND));
        return subjectComponentMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectComponentResponse> getComponentsBySubjectId(UUID subjectId) {
        List<SubjectComponent> entities = subjectComponentRepository.findBySubjectId(subjectId);
        return subjectComponentMapper.toResponseList(entities);
    }

    @Override
    @Transactional
    public void deleteSubjectComponent(UUID id) {
        SubjectComponent entity = subjectComponentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND));
        // Hard delete for components as they don't have isActive flag in model
        subjectComponentRepository.delete(entity);
    }
}
