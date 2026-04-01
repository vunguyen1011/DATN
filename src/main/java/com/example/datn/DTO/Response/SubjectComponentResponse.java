package com.example.datn.DTO.Response;

import com.example.datn.ENUM.ComponentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectComponentResponse {

    private UUID id;

    private UUID subjectId;
    
    private String subjectName;
    
    private String subjectCode;

    private ComponentType type;

    private UUID requiredRoomTypeId;
    
    private String requiredRoomTypeName;

    private Integer sessionsPerWeek;

    private Integer periodsPerSession;

    private Integer totalPeriods;

    private Double weightPercent;

    private Integer numberCredit;
}
