package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.PeriodCohortRequest;
import com.example.datn.DTO.Request.PeriodCohortUpdateRequest;
import com.example.datn.DTO.Response.PeriodCohortResponse;

import java.util.List;
import java.util.UUID;

public interface IPeriodCohortService {
    PeriodCohortResponse create(PeriodCohortRequest request);
    PeriodCohortResponse getById(UUID id);
    List<PeriodCohortResponse> getAll();
    List<PeriodCohortResponse> getByRegistrationPeriodId(UUID periodId);
    PeriodCohortResponse update(UUID id, PeriodCohortUpdateRequest request);
    void delete(UUID id);
    com.example.datn.Model.PeriodCohort getOngoingCohortPeriod(UUID cohortId, java.time.LocalDateTime currentTime);
}
