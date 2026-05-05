package com.example.datn.DTO.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EnrollRequest {
    @NotNull(message = "theoryClassId is required")
    private UUID theoryClassId;
    
    private UUID labClassId;
}
