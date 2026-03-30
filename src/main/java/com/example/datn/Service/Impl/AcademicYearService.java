package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.AcademicYearRequest;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.AcademicYearMapper;
import com.example.datn.Model.AcademicYear;
import com.example.datn.Repository.AcademicYearRepository;
import com.example.datn.Service.Interface.IAcademicYearService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicYearService implements IAcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final AcademicYearMapper academicYearMapper;

    @Override
    @Transactional
    public AcademicYear createAcademicYear(AcademicYearRequest request) {
        if (academicYearRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_NAME_EXISTED);
        }

        // Logic "Ăn điểm": Nếu năm mới tạo được set là "Hiện tại", phải tắt các năm cũ đi
        if (Boolean.TRUE.equals(request.getIsCurrent())) {
            academicYearRepository.resetAllCurrentToFalse();
        }

        AcademicYear newAcademicYear = academicYearMapper.toEntity(request);
        return academicYearRepository.save(newAcademicYear);
    }

    @Override
    public List<AcademicYear> getAllAcademicYears() {
        return academicYearRepository.findAll();
    }

    @Override
    public AcademicYear getAcademicYearById(UUID id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND));
    }

    @Override
    @Transactional
    public AcademicYear updateAcademicYear(UUID id, AcademicYearRequest request) {
        AcademicYear existingAcademicYear = getAcademicYearById(id);
        if (!existingAcademicYear.getName().equals(request.getName()) && academicYearRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_NAME_EXISTED);
        }
        if (Boolean.TRUE.equals(request.getIsCurrent())) {
            academicYearRepository.resetAllCurrentToFalse();
        }

        academicYearMapper.updateAcademicYearFromRequest(existingAcademicYear, request);
        return academicYearRepository.save(existingAcademicYear);
    }

    @Override
    @Transactional
    public void deleteAcademicYear(UUID id) {
        AcademicYear existingAcademicYear = getAcademicYearById(id);
        academicYearRepository.delete(existingAcademicYear);
    }

    @Override
    public List<AcademicYear> searchAcademicYears(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAcademicYears();
        }
        return academicYearRepository.findByNameContainingIgnoreCase(keyword.trim());
    }
}