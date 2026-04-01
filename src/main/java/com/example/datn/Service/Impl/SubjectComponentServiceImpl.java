package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SubjectComponentRequest;
import com.example.datn.DTO.Response.SubjectComponentResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SubjectComponentMapper;
import com.example.datn.Model.RoomType;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectComponent;
import com.example.datn.Repository.SubjectComponentRepository;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Repository.TypeRoomRepository;
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
    private final SubjectRepository subjectRepository;
    private final TypeRoomRepository roomTypeRepository;
    private final SubjectComponentMapper subjectComponentMapper;

    @Override
    @Transactional
    public SubjectComponentResponse createSubjectComponent(SubjectComponentRequest request) {
        calculateTotalPeriods(request);

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        RoomType roomType = null;
        if (request.getRequiredRoomTypeId() != null) {
            roomType = roomTypeRepository.findById(request.getRequiredRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        }

        SubjectComponent entity = subjectComponentMapper.toEntity(request, subject, roomType);
        return subjectComponentMapper.toResponse(subjectComponentRepository.save(entity));
    }

    @Override
    @Transactional
    public SubjectComponentResponse updateSubjectComponent(UUID id, SubjectComponentRequest request) {
        SubjectComponent entity = subjectComponentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_COMPONENT_NOT_FOUND));

        calculateTotalPeriods(request);

        Subject subject = subjectRepository.findByIdAndIsActiveTrue(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        RoomType roomType = null;
        if (request.getRequiredRoomTypeId() != null) {
            roomType = roomTypeRepository.findById(request.getRequiredRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        }

        subjectComponentMapper.updateEntityFromRequest(entity, request, subject, roomType);
        return subjectComponentMapper.toResponse(subjectComponentRepository.save(entity));
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
        subjectComponentRepository.delete(entity);
    }

    private void calculateTotalPeriods(SubjectComponentRequest request) {
        if (request.getTotalPeriods() == null || request.getTotalPeriods() <= 0) {
            if (request.getSessionsPerWeek() != null && request.getPeriodsPerSession() != null) {
                request.setTotalPeriods(request.getSessionsPerWeek() * request.getPeriodsPerSession() * 15);
            }
        }
    }
}