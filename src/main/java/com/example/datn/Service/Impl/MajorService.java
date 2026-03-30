package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.MajorRequest;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.MajorMapper;
import com.example.datn.Model.Major;
import com.example.datn.Repository.MajorRepository;
import com.example.datn.Service.Interface.IMajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MajorService implements IMajorService {

    private final MajorRepository majorRepository;
    private final MajorMapper majorMapper;

    @Override
    @Transactional
    public Major createMajor(MajorRequest request) {
        // 1. Kiểm tra trùng lặp Mã ngành
        if (majorRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.MAJOR_CODE_EXISTED);
        }
        // 2. Kiểm tra trùng lặp Tên ngành
        if (majorRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.MAJOR_NAME_EXISTED);
        }
        // 3. Map từ Request sang Entity và Lưu
        Major newMajor = majorMapper.toEntity(request);
        return majorRepository.save(newMajor);
    }

    @Override
    public List<Major> getAllMajors() {
        return majorRepository.findAll();
    }

    @Override
    public Major getMajorById(UUID id) {
        return majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));
    }

    @Override
    @Transactional
    public Major updateMajor(UUID id, MajorRequest request) {
        // 1. Tìm ngành học hiện tại (Nếu không thấy sẽ tự ném ra MAJOR_NOT_FOUND nhờ hàm trên)
        Major existingMajor = getMajorById(id);
        // 2. Kiểm tra xem Mã code mới có bị trùng với ngành KHÁC không
        if (!existingMajor.getCode().equals(request.getCode()) && majorRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.MAJOR_CODE_EXISTED);
        }
        // 3. Tương tự, kiểm tra Tên ngành mới
        if (!existingMajor.getName().equals(request.getName()) && majorRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.MAJOR_NAME_EXISTED);
        }
        // 4. Đổ dữ liệu mới vào Entity cũ
        majorMapper.updateMajorFromRequest(existingMajor, request);
        // 5. Lưu lại
        return majorRepository.save(existingMajor);
    }

    @Override
    @Transactional
    public void deleteMajor(UUID id) {
        // Nếu không thấy sẽ tự ném ra MAJOR_NOT_FOUND
        Major existingMajor = getMajorById(id);
        existingMajor.setIsActive(false);
        majorRepository.save(existingMajor);
    }

    @Override
    public List<Major> searchMajors(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllMajors();
        }
        return majorRepository.findByNameContainingIgnoreCase(keyword.trim());
    }
}