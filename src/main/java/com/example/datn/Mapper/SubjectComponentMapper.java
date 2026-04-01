package com.example.datn.Mapper;

import com.example.datn.DTO.Request.SubjectComponentRequest;
import com.example.datn.DTO.Response.SubjectComponentResponse;
import com.example.datn.Model.RoomType;
import com.example.datn.Model.Subject;
import com.example.datn.Model.SubjectComponent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubjectComponentMapper {

    public SubjectComponent toEntity(SubjectComponentRequest request, Subject subject, RoomType roomType) {
        if (request == null) return null;

        return SubjectComponent.builder()
                .subject(subject)
                .type(request.getType())
                .requiredRoomType(roomType)
                .sessionsPerWeek(request.getSessionsPerWeek())
                .periodsPerSession(request.getPeriodsPerSession())
                .totalPeriods(request.getTotalPeriods())
                .weightPercent(request.getWeightPercent())
                .build();
    }

    public void updateEntityFromRequest(SubjectComponent entity, SubjectComponentRequest request, Subject subject, RoomType roomType) {
        if (request == null || entity == null) return;

        entity.setSubject(subject);
        entity.setType(request.getType());
        entity.setRequiredRoomType(roomType);
        entity.setSessionsPerWeek(request.getSessionsPerWeek());
        entity.setPeriodsPerSession(request.getPeriodsPerSession());
        entity.setTotalPeriods(request.getTotalPeriods());
        entity.setWeightPercent(request.getWeightPercent());
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
                .build();
    }

    public List<SubjectComponentResponse> toResponseList(List<SubjectComponent> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }
}