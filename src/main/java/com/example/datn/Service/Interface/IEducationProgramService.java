package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.EducationProgramRequest;
import com.example.datn.DTO.Request.ProgramCohortRequest;
import com.example.datn.DTO.Response.EducationProgramResponse;
import com.example.datn.DTO.Response.ProgramCohortResponse;
import com.example.datn.DTO.Response.ProgramTreeResponse;

import java.util.List;
import java.util.UUID;

public interface IEducationProgramService {
    // CRUD chương trình đào tạo
    EducationProgramResponse createProgram(EducationProgramRequest request);
    EducationProgramResponse updateProgram(UUID id, EducationProgramRequest request);
    void softDeleteProgram(UUID id);
    EducationProgramResponse getProgramById(UUID id);
    List<EducationProgramResponse> getAllPrograms(String param);

    // Publish (khóa) chương trình đào tạo
    EducationProgramResponse publishProgram(UUID id);

    // Cây cấu trúc chương trình
    ProgramTreeResponse getProgramTree(UUID programId);

    // Gắn / gỡ chương trình đào tạo vào khóa học
    ProgramCohortResponse assignProgramToCohort(ProgramCohortRequest request);
    void removeProgramFromCohort(UUID programId, UUID cohortId);
    List<ProgramCohortResponse> getCohortsByProgram(UUID programId);
    List<ProgramCohortResponse> getProgramsByCohort(UUID cohortId);
}

