package com.example.datn.DTO.Response;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResponse {

    private UUID id;

    private String code;

    private String name;

    private Integer credits;

    private String departmentName;

    private Integer theoryPeriod;

    private Integer practicePeriod;

    private Boolean isActive;
    private Double coefficient;
}