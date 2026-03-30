package com.example.datn.Service.Impl;

import com.example.datn.DTO.Request.SubjectGroupRequest;
import com.example.datn.DTO.Response.SubjectGroupResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.SubjectGroupMapper;
import com.example.datn.Model.SubjectGroup;
import com.example.datn.Repository.SubjectGroupRepository;
import com.example.datn.Service.Interface.ISubjectGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectGroupService implements ISubjectGroupService {

    private final SubjectGroupRepository subjectGroupRepository;
    private final SubjectGroupMapper subjectGroupMapper;

    // Đã xóa EducationProgramRepository vì không sử dụng đến, giúp code sạch và tối ưu hơn

    @Override
    @Transactional
    public SubjectGroupResponse createGroup(SubjectGroupRequest request) {
        // 1. Kiểm tra trùng lặp tên nhóm môn học
        if (subjectGroupRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.SUBJECT_GROUP_ALREADY_EXISTS);
        }

        // 2. Map từ DTO sang Entity
        SubjectGroup group = subjectGroupMapper.toEntity(request);

        // 3. Set trạng thái mặc định là Active khi mới tạo
        group.setIsActive(true);

        return subjectGroupMapper.toResponse(subjectGroupRepository.save(group));
    }

    @Override
    @Transactional
    public SubjectGroupResponse updateGroup(UUID id, SubjectGroupRequest request) {
        // 1. Lấy dữ liệu (Chỉ lấy những group chưa bị xóa mềm)
        SubjectGroup existingGroup = subjectGroupRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_GROUP_NOT_FOUND));

        // 2. Kiểm tra trùng tên (Chỉ check nếu user đổi tên group sang một tên khác)
        if (!existingGroup.getName().equals(request.getName()) &&
                subjectGroupRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.SUBJECT_GROUP_ALREADY_EXISTS);
        }

        // 3. Đổ dữ liệu mới vào Entity cũ
        subjectGroupMapper.updateEntity(existingGroup, request);

        return subjectGroupMapper.toResponse(subjectGroupRepository.save(existingGroup));
    }

    @Override
    public SubjectGroupResponse getGroupById(UUID id) {
        // Tránh lấy lên những group đã bị xóa mềm
        SubjectGroup group = subjectGroupRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_GROUP_NOT_FOUND));
        return subjectGroupMapper.toResponse(group);
    }

    @Override
    public List<SubjectGroupResponse> getAllActiveGroups() {
        return subjectGroupMapper.toResponseList(subjectGroupRepository.findByIsActiveTrue());
    }

    @Override
    @Transactional
    public void softDeleteGroup(UUID id) {
        // Chỉ xóa những group đang còn tồn tại và active
        SubjectGroup group = subjectGroupRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_GROUP_NOT_FOUND));
        group.setIsActive(false); // Xóa mềm
        subjectGroupRepository.save(group);
    }
}