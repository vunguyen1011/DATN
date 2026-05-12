package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.RegistrationPeriodRequest;
import com.example.datn.DTO.Request.RegistrationPeriodUpdateRequest;
import com.example.datn.DTO.Response.RegistrationPeriodResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.RegistrationPeriodMapper;
import com.example.datn.Model.PeriodCohort;
import com.example.datn.Model.RegistrationPeriod;
import com.example.datn.Model.Semester;
import com.example.datn.Repository.PeriodCohortRepository;
import com.example.datn.Repository.RegistrationPeriodRepository;
import com.example.datn.Repository.SemesterRepository;
import com.example.datn.Service.Interface.IRegistrationPeriodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationPeriodServiceImpl implements IRegistrationPeriodService {

    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final SemesterRepository semesterRepository;
    private final RegistrationPeriodMapper registrationPeriodMapper;
    private final PeriodCohortRepository periodCohortRepository;

    @Override
    @Transactional
    public RegistrationPeriodResponse createRegistrationPeriod(RegistrationPeriodRequest request) {
        log.info("[RegistrationPeriodService] Tạo đợt đăng ký: {}", request.getName());
        Semester semester= semesterRepository.findByIsCurrentTrue().orElseThrow(()-> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không tìm thấy học kỳ hiện tại"));


        validateTimeRange(request);

        if (registrationPeriodRepository.existsBySemesterIdAndName(semester.getId(), request.getName())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên đợt đăng ký đã tồn tại trong học kỳ này");
        }

        long overlapCount = registrationPeriodRepository.countOverlappingPeriods(
                semester.getId(), request.getStartTime(), request.getEndTime());
        if (overlapCount > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thời gian của đợt đăng ký này bị trùng lặp với đợt khác");
        }

        RegistrationPeriod entity = registrationPeriodMapper.toEntity(request, semester);
        RegistrationPeriod saved = registrationPeriodRepository.save(entity);

        return registrationPeriodMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RegistrationPeriodResponse updateRegistrationPeriod(UUID id, RegistrationPeriodUpdateRequest request) {
        log.info("[RegistrationPeriodService] Cập nhật đợt đăng ký: {}", id);

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thời gian bắt đầu và kết thúc không được để trống");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE, "Thời gian kết thúc phải diễn ra sau thời gian bắt đầu");
        }

        RegistrationPeriod registrationPeriod = registrationPeriodRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không tìm thấy đợt đăng ký"));

        UUID semesterId = registrationPeriod.getSemester().getId();

        long overlapCount = registrationPeriodRepository.countOverlappingPeriodsExcludingId(
                semesterId, request.getStartTime(), request.getEndTime(), id);

        if (overlapCount > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thời gian của đợt đăng ký này bị trùng lặp với đợt khác");
        }


        List<PeriodCohort> children = periodCohortRepository.findByRegistrationPeriodId(id);

        if (children != null && !children.isEmpty()) {
            for (var  child : children) {
                if (child.getStartTime().isBefore(request.getStartTime()) ||
                        child.getEndTime().isAfter(request.getEndTime())) {
                    throw new AppException(ErrorCode.INVALID_DATE_RANGE,
                            "Thời gian không hợp lệ: Đợt con bị nằm ngoài khoảng thời gian của đợt cha");
                }
            }
        }

        registrationPeriod.setStartTime(request.getStartTime());
        registrationPeriod.setEndTime(request.getEndTime());

        return registrationPeriodMapper.toResponse(registrationPeriodRepository.save(registrationPeriod));
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

    private void validateTimeRange(RegistrationPeriodRequest request) {
        // 1. Kiểm tra NULL trước
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thời gian bắt đầu và kết thúc không được để trống");
        }

        // 2. Sau khi chắc chắn không null, mới kiểm tra thời gian quá khứ
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thời gian bắt đầu không được diễn ra trong quá khứ");
        }

        // 3. Kiểm tra tính hợp lệ của khoảng thời gian
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE, "Thời gian kết thúc phải diễn ra sau thời gian bắt đầu");
        }
    }
}