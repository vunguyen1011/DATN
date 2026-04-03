package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.LecturerUpdateRequest;
import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.LecturerMapper;
import com.example.datn.Model.Lecturer;
import com.example.datn.Repository.LecturerRepository;
import com.example.datn.Service.Interface.ILecturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LecturerService implements ILecturerService {
    
    private final LecturerRepository lecturerRepository;

    @Override
    @Transactional
    public UserProfileResponse.LecturerProfile updateLecturerProfile(UUID lecturerId, LecturerUpdateRequest request) {
        Lecturer lecturer = lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)); 
        
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            lecturer.setFullName(request.getFullName().trim());
            if (lecturer.getUser() != null) {
                lecturer.getUser().setFullName(request.getFullName().trim());
            }
        }
        if (request.getPhone() != null) lecturer.setPhone(request.getPhone());
        if (request.getAddress() != null) lecturer.setAddress(request.getAddress());
        if (request.getDegree() != null) lecturer.setDegree(request.getDegree());
        if (request.getStatus() != null) lecturer.setStatus(request.getStatus());
        
        lecturer = lecturerRepository.save(lecturer);
        
        return LecturerMapper.toLecturerProfile(lecturer);
    }
}
