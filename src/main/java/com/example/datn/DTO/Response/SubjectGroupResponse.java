package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SubjectGroupResponse {
    private UUID id;
    private String name;
    private Boolean isActive;
    private Boolean isGlobal;
    private int index;
}