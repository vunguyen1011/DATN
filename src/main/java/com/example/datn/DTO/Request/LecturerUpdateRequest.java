package com.example.datn.DTO.Request;

import com.example.datn.ENUM.LecturerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LecturerUpdateRequest {
    private String fullName;
    private String phone;
    private String address;
    private String degree;
    
    // Admin fields
    private LecturerStatus status;
}
