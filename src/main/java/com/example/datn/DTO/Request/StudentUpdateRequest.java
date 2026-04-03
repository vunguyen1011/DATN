package com.example.datn.DTO.Request;

import com.example.datn.ENUM.Gender;
import com.example.datn.ENUM.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentUpdateRequest {
    private String fullName;
    private String phone;
    private String address;
    private Gender gender;
    
    // Admin fields
    private StudentStatus status;
    private UUID cohortId;
    private UUID majorId;
    private UUID adminClassId;
}
