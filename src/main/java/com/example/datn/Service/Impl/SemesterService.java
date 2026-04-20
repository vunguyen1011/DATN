package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SemesterRequest;
import com.example.datn.DTO.Response.SemesterResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SemesterMapper;
import com.example.datn.Model.AcademicYear;
import com.example.datn.Model.Semester;
import com.example.datn.Repository.AcademicYearRepository;
import com.example.datn.Repository.SemesterRepository;
import com.example.datn.Service.Interface.ISemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemesterService implements ISemesterService {

    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterMapper semesterMapper;

    private Semester findEntityById(UUID id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));
    }

    // Logic dùng chung: Kiểm tra ngày hợp lệ
    private void validateDates(SemesterRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE); // Ngày bắt đầu không được lớn hơn ngày kết thúc
        }
    }

    @Override
    @Transactional
    public SemesterResponse createSemester(SemesterRequest request) {
        validateDates(request);

        // Kiểm tra xem Năm học đó đã có Học kỳ này chưa
        if (semesterRepository.existsByNameAndAcademicYearId(request.getName(), request.getAcademicYearId())) {
            throw new AppException(ErrorCode.SEMESTER_NAME_EXISTED_IN_YEAR);
        }

        AcademicYear academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND));

        Semester newSemester = semesterMapper.toEntity(request, academicYear);
        Semester oldSemester = semesterRepository.findByIsCurrentTrue().orElseThrow(()->new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
        oldSemester.setIsCurrent(false);
        semesterRepository.save(oldSemester);

        Semester savedSemester = semesterRepository.save(newSemester);

        return semesterMapper.toResponse(savedSemester);
    }

    @Override
    public List<SemesterResponse> getAllSemesters() {
        return semesterRepository.findAll().stream()
                .map(semesterMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SemesterResponse getSemesterById(UUID id) {
        return semesterMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public SemesterResponse updateSemester(UUID id, SemesterRequest request) {
        validateDates(request);
        Semester existingSemester = findEntityById(id);

        // Kiểm tra trùng lặp nếu người dùng đổi tên hoặc đổi năm học
        boolean isNameChanged = !existingSemester.getName().equals(request.getName());
        boolean isYearChanged = !existingSemester.getAcademicYear().getId().equals(request.getAcademicYearId());

        if ((isNameChanged || isYearChanged) &&
                semesterRepository.existsByNameAndAcademicYearId(request.getName(), request.getAcademicYearId())) {
            throw new AppException(ErrorCode.SEMESTER_NAME_EXISTED_IN_YEAR);
        }

        AcademicYear academicYear = null;
        if (isYearChanged) {
            academicYear = academicYearRepository.findById(request.getAcademicYearId())
                    .orElseThrow(() -> new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND));
        }

        semesterMapper.updateSemesterFromRequest(existingSemester, request, academicYear);
        Semester updatedSemester = semesterRepository.save(existingSemester);

        return semesterMapper.toResponse(updatedSemester);
    }

    @Override
    @Transactional
    public void deleteSemester(UUID id) {
        semesterRepository.delete(findEntityById(id));
    }

    @Override
    public List<SemesterResponse> searchSemesters(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSemesters();
        }
        return semesterRepository.findByNameContainingIgnoreCase(keyword.trim()).stream()
                .map(semesterMapper::toResponse)
                .collect(Collectors.toList());
    }
    @Override
    public SemesterResponse findCurrentSemester() {
        Semester current= semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
        return semesterMapper.toResponse(current);
    }

}