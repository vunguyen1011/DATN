package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.EducationProgramRequest;
import com.example.datn.DTO.Response.EducationProgramResponse;
import com.example.datn.DTO.Response.ProgramTreeResponse;

import java.util.List;
import java.util.UUID;

public interface IEducationProgramService {
    EducationProgramResponse createProgram(EducationProgramRequest request);
    ProgramTreeResponse getProgramTree(UUID programId);
        List<EducationProgramResponse> getAllPrograms(String param);


}
