package com.example.datn.DTO.Response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ProgramCohortResponse {
    private UUID id;

    // Thông tin chương trình đào tạo
    private UUID programId;
    private String programCode;
    private String programName;

    // Thông tin khóa học
    private UUID cohortId;
    private String cohortName;
    private Integer cohortStartYear;

    private LocalDate appliedDate;
    private Boolean isActiveForCohort;
}
