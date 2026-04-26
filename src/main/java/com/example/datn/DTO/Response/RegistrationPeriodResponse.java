package com.example.datn.DTO.Response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationPeriodResponse {

    private UUID id;
    private UUID semesterId;
    private String semesterName;
    private String name;
    private Boolean isActive;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
