package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.PeriodCohortRequest;
import com.example.datn.DTO.Request.PeriodCohortUpdateRequest;
import com.example.datn.DTO.Response.PeriodCohortResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Model.Cohort;
import com.example.datn.Model.PeriodCohort;
import com.example.datn.Model.RegistrationPeriod;
import com.example.datn.Repository.CohortRepository;
import com.example.datn.Repository.PeriodCohortRepository;
import com.example.datn.Repository.RegistrationPeriodRepository;
import com.example.datn.Service.Interface.IPeriodCohortService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeriodCohortServiceImpl implements IPeriodCohortService {

    private final PeriodCohortRepository periodCohortRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final CohortRepository cohortRepository;

    @Override
    @Transactional
    public PeriodCohortResponse create(PeriodCohortRequest request) {
        RegistrationPeriod registrationPeriod = registrationPeriodRepository.findById(request.getRegistrationPeriodId())
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND)); // You might want to add REGISTRATION_PERIOD_NOT_FOUND

        Cohort cohort = null;
        if (request.getCohortId() != null) {
            cohort = cohortRepository.findById(request.getCohortId())
                    .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        }
        checkOverlap(request.getCohortId(), request.getStartTime(), request.getEndTime(), null);
        PeriodCohort periodCohort = PeriodCohort.builder()
                .registrationPeriod(registrationPeriod)
                .cohort(cohort)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        periodCohort = periodCohortRepository.save(periodCohort);
        return mapToResponse(periodCohort);
    }

    @Override
    public PeriodCohortResponse getById(UUID id) {
        PeriodCohort periodCohort = periodCohortRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERIOD_COHORT_NOT_FOUND));
        return mapToResponse(periodCohort);
    }

    @Override
    public List<PeriodCohortResponse> getAll() {
        return periodCohortRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PeriodCohortResponse> getByRegistrationPeriodId(UUID periodId) {
        return periodCohortRepository.findByRegistrationPeriodId(periodId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PeriodCohortResponse update(UUID id, PeriodCohortUpdateRequest request) {
        // 1. Tìm bản ghi cũ
        PeriodCohort periodCohort = periodCohortRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy cấu hình thời gian"));

        // 2. Chỉ cập nhật thời gian
        periodCohort.setStartTime(request.getStartTime());
        periodCohort.setEndTime(request.getEndTime());

        // 3. Lưu lại
        return mapToResponse(periodCohortRepository.save(periodCohort));
    }
    @Override
    @Transactional
    public void delete(UUID id) {
        if (!periodCohortRepository.existsById(id)) {
            throw new AppException(ErrorCode.PERIOD_COHORT_NOT_FOUND);
        }
        periodCohortRepository.deleteById(id);
    }

    private PeriodCohortResponse mapToResponse(PeriodCohort periodCohort) {
        return PeriodCohortResponse.builder()
                .id(periodCohort.getId())
                .registrationPeriodId(periodCohort.getRegistrationPeriod().getId())
                .registrationPeriodName(periodCohort.getRegistrationPeriod().getName())
                .cohortId(periodCohort.getCohort() != null ? periodCohort.getCohort().getId() : null)
                .cohortName(periodCohort.getCohort() != null ? periodCohort.getCohort().getName() : null)
                .startTime(periodCohort.getStartTime())
                .endTime(periodCohort.getEndTime())
                .build();
    }
    private void checkOverlap(UUID cohortId, LocalDateTime startTime, LocalDateTime endTime, UUID excludeId) {
        boolean isOverlap = periodCohortRepository.existsOverlappingPeriod(cohortId, startTime, endTime, excludeId);
        if (isOverlap) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thời gian đăng ký bị trùng lặp với một cấu hình đã có của khóa này");
        }
    }
}
