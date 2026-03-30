package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.CohortRequest;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.CohortMapper;
import com.example.datn.Model.Cohort;
import com.example.datn.Repository.CohortRepository;
import com.example.datn.Service.Interface.ICohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CohortService implements ICohortService {

    private final CohortRepository cohortRepository;
    private final CohortMapper cohortMapper;

    @Override
    @Transactional
    public Cohort createCohort(CohortRequest request) {
        if (cohortRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.COHORT_NAME_EXISTED);
        }
        Cohort newCohort = cohortMapper.toEntity(request);
        return cohortRepository.save(newCohort);
    }

    @Override
    public List<Cohort> getAllCohorts() {
        return cohortRepository.findAll();
    }

    @Override
    public Cohort getCohortById(UUID id) {
        return cohortRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
    }

    @Override
    @Transactional
    public Cohort updateCohort(UUID id, CohortRequest request) {
        Cohort existingCohort = getCohortById(id);

        if (!existingCohort.getName().equals(request.getName()) && cohortRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.COHORT_NAME_EXISTED);
        }

        cohortMapper.updateCohortFromRequest(existingCohort, request);
        return cohortRepository.save(existingCohort);
    }

    @Override
    @Transactional
    public void deleteCohort(UUID id) {
        Cohort existingCohort = getCohortById(id);
        // Có thể bổ sung check xem khóa này có sinh viên nào chưa trước khi xóa
        cohortRepository.delete(existingCohort);
    }

    @Override
    public List<Cohort> searchCohorts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCohorts();
        }
        return cohortRepository.findByNameContainingIgnoreCase(keyword.trim());
    }
}