package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.CohortRequest;
import com.example.datn.Model.Cohort;

import java.util.List;
import java.util.UUID;

public interface ICohortService {
    Cohort createCohort(CohortRequest request);
    List<Cohort> getAllCohorts();
    Cohort getCohortById(UUID id);
    Cohort updateCohort(UUID id, CohortRequest request);
    void deleteCohort(UUID id);
    List<Cohort> searchCohorts(String keyword);
}