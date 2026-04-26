package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.RegistrationPeriodRequest;
import com.example.datn.DTO.Response.RegistrationPeriodResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.RegistrationPeriodMapper;
import com.example.datn.Model.RegistrationPeriod;
import com.example.datn.Model.Semester;
import com.example.datn.Repository.RegistrationPeriodRepository;
import com.example.datn.Repository.SemesterRepository;
import com.example.datn.Service.Interface.IRegistrationPeriodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationPeriodServiceImpl implements IRegistrationPeriodService {

    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final SemesterRepository semesterRepository;
    private final RegistrationPeriodMapper registrationPeriodMapper;

    @Override
    @Transactional
    public RegistrationPeriodResponse createRegistrationPeriod(RegistrationPeriodRequest request) {
        log.info("[RegistrationPeriodService] Tạo đợt đăng ký: {}", request.getName());

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        if (request.getStartTime() != null && request.getEndTime() != null &&
                request.getStartTime().isAfter(request.getEndTime())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        RegistrationPeriod entity = registrationPeriodMapper.toEntity(request, semester);
        RegistrationPeriod saved = registrationPeriodRepository.save(entity);

        return registrationPeriodMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RegistrationPeriodResponse updateRegistrationPeriod(UUID id, RegistrationPeriodRequest request) {
        log.info("[RegistrationPeriodService] Cập nhật đợt đăng ký: {}", id);

        RegistrationPeriod entity = registrationPeriodRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không tìm thấy đợt đăng ký"));

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        if (request.getStartTime() != null && request.getEndTime() != null &&
                request.getStartTime().isAfter(request.getEndTime())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        registrationPeriodMapper.updateEntityFromRequest(entity, request, semester);
        RegistrationPeriod updated = registrationPeriodRepository.save(entity);

        return registrationPeriodMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRegistrationPeriod(UUID id) {
        log.info("[RegistrationPeriodService] Xóa đợt đăng ký: {}", id);
        RegistrationPeriod entity = registrationPeriodRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không tìm thấy đợt đăng ký"));
        registrationPeriodRepository.delete(entity);
    }

    @Override
    public RegistrationPeriodResponse getRegistrationPeriodById(UUID id) {
        RegistrationPeriod entity = registrationPeriodRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không tìm thấy đợt đăng ký"));
        return registrationPeriodMapper.toResponse(entity);
    }

    @Override
    public Page<RegistrationPeriodResponse> getAllRegistrationPeriods(Pageable pageable) {
        return registrationPeriodRepository.findAll(pageable)
                .map(registrationPeriodMapper::toResponse);
    }

    @Override
    public Page<RegistrationPeriodResponse> getRegistrationPeriodsBySemester(UUID semesterId, Pageable pageable) {
        return registrationPeriodRepository.findBySemesterId(semesterId, pageable)
                .map(registrationPeriodMapper::toResponse);
    }
}
