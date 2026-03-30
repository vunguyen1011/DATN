package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.AdminClassRequest;
import com.example.datn.DTO.Response.AdminClassResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.AdminClassMapper;
import com.example.datn.Model.AdminClass;
import com.example.datn.Model.Cohort;
import com.example.datn.Model.Major;
import com.example.datn.Repository.AdminClassRepository;
import com.example.datn.Repository.CohortRepository;
import com.example.datn.Repository.MajorRepository;
import com.example.datn.Service.Interface.IAdminClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminClassService implements IAdminClassService {

    private final AdminClassRepository adminClassRepository;
    private final MajorRepository majorRepository;
    private final CohortRepository cohortRepository;
    private final AdminClassMapper adminClassMapper;

    // Helper method nội bộ để lấy Entity phục vụ cho các logic khác
    private AdminClass findEntityById(UUID id) {
        return adminClassRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_CLASS_NOT_FOUND));
    }

    @Override
    @Transactional
    public AdminClassResponse createAdminClass(AdminClassRequest request) {

        if (adminClassRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.ADMIN_CLASS_NAME_EXISTED);
        }

        Major major = majorRepository.findById(request.getMajorId())
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));

        AdminClass newAdminClass = adminClassMapper.toEntity(request, major, cohort);
        AdminClass savedClass = adminClassRepository.save(newAdminClass);

        // Trả về Response DTO
        return adminClassMapper.toResponse(savedClass);
    }

    @Override
    public List<AdminClassResponse> getAllAdminClasses() {
        return adminClassRepository.findAll()
                .stream()
                .map(adminClassMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AdminClassResponse getAdminClassById(UUID id) {
        AdminClass entity = findEntityById(id);
        return adminClassMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public AdminClassResponse updateAdminClass(UUID id, AdminClassRequest request) {
        AdminClass existingClass = findEntityById(id);


        if (!existingClass.getName().equals(request.getName()) && adminClassRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.ADMIN_CLASS_NAME_EXISTED);
        }

        Major major = null;
        if (!existingClass.getMajor().getId().equals(request.getMajorId())) {
            major = majorRepository.findById(request.getMajorId())
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));
        }

        Cohort cohort = null;
        if (!existingClass.getCohort().getId().equals(request.getCohortId())) {
            cohort = cohortRepository.findById(request.getCohortId())
                    .orElseThrow(() -> new AppException(ErrorCode.COHORT_NOT_FOUND));
        }

        adminClassMapper.updateAdminClassFromRequest(existingClass, request, major, cohort);
        AdminClass updatedClass = adminClassRepository.save(existingClass);

        return adminClassMapper.toResponse(updatedClass);
    }

    @Override
    @Transactional
    public void deleteAdminClass(UUID id) {
        AdminClass existingClass = findEntityById(id);
        adminClassRepository.delete(existingClass);
    }

    @Override
    public List<AdminClassResponse> searchAdminClasses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAdminClasses();
        }
        return adminClassRepository.searchByName(keyword.trim())
                .stream()
                .map(adminClassMapper::toResponse)
                .collect(Collectors.toList());
    }
}