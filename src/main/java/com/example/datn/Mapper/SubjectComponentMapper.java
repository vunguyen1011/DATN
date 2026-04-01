package com.example.datn.Mapper;

import com.example.datn.DTO.Request.SubjectComponentRequest;
import com.example.datn.DTO.Response.SubjectComponentResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.RoomType;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectComponent;
import com.example.datn.Repository.SubjectRepository;
import com.example.datn.Repository.TypeRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubjectComponentMapper {

    private final SubjectRepository subjectRepository;
    private final TypeRoomService roomTypeRepository;

    public SubjectComponent toEntity(SubjectComponentRequest request) {
        if (request == null) return null;

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        RoomType roomType = null;
        if (request.getRequiredRoomTypeId() != null) {
            roomType = roomTypeRepository.findById(request.getRequiredRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        }

        return SubjectComponent.builder()
                .subject(subject)
                .type(request.getType())
                .requiredRoomType(roomType)
                .sessionsPerWeek(request.getSessionsPerWeek())
                .periodsPerSession(request.getPeriodsPerSession())
                .totalPeriods(request.getTotalPeriods())
                .weightPercent(request.getWeightPercent())
                .numberCredit(request.getNumberCredit())
                .build();
    }

    public void updateEntityFromRequest(SubjectComponent entity, SubjectComponentRequest request) {
        if (request == null || entity == null) return;

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        RoomType roomType = null;
        if (request.getRequiredRoomTypeId() != null) {
            roomType = roomTypeRepository.findById(request.getRequiredRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        }

        entity.setSubject(subject);
        entity.setType(request.getType());
        entity.setRequiredRoomType(roomType);
        entity.setSessionsPerWeek(request.getSessionsPerWeek());
        entity.setPeriodsPerSession(request.getPeriodsPerSession());
        entity.setTotalPeriods(request.getTotalPeriods());
        entity.setWeightPercent(request.getWeightPercent());
        entity.setNumberCredit(request.getNumberCredit());
    }

    public SubjectComponentResponse toResponse(SubjectComponent entity) {
        if (entity == null) return null;

        return SubjectComponentResponse.builder()
                .id(entity.getId())
                .subjectId(entity.getSubject() != null ? entity.getSubject().getId() : null)
                .subjectName(entity.getSubject() != null ? entity.getSubject().getName() : null)
                .subjectCode(entity.getSubject() != null ? entity.getSubject().getCode() : null)
                .type(entity.getType())
                .requiredRoomTypeId(entity.getRequiredRoomType() != null ? entity.getRequiredRoomType().getId() : null)
                .requiredRoomTypeName(entity.getRequiredRoomType() != null ? entity.getRequiredRoomType().getName() : null)
                .sessionsPerWeek(entity.getSessionsPerWeek())
                .periodsPerSession(entity.getPeriodsPerSession())
                .totalPeriods(entity.getTotalPeriods())
                .weightPercent(entity.getWeightPercent())
                .numberCredit(entity.getNumberCredit())
                .build();
    }

    public List<SubjectComponentResponse> toResponseList(List<SubjectComponent> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
