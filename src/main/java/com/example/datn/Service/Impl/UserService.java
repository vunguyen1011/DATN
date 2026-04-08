package com.example.datn.Service.Impl;

import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.UserMapper;
import com.example.datn.Model.*;
import com.example.datn.Repository.*;
import com.example.datn.Service.Interface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository repository;

    // Hàm private dùng chung để lấy thông tin chi tiết Profile
    private UserProfileResponse getUserProfileDetail(User user) {
        List<UserRole> roles = userRoleRepository.findByUser(user);
        Set<Role> roleSet = roles.stream().map(UserRole::getRole).collect(Collectors.toSet());
        Lecturer lecturer = lecturerRepository.findByUser(user).orElse(null);
        Student student = studentRepository.findByUser(user).orElse(null);

        return UserMapper.toUserProfileResponse(user, student, lecturer, roleSet);
    }

    @Override
    public UserProfileResponse getMyInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return getUserProfileDetail(user);
    }

    @Override
    public UserProfileResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return getUserProfileDetail(user);
    }

    @Override
    public Page<UserProfileResponse> getAllUsers(String searchKeyword, Pageable pageable) {
        Page<User> users;
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            users = userRepository.searchUsers(searchKeyword.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::getUserProfileDetail);
    }

    @Override
    @Transactional
    public UserProfileResponse toggleUserStatus(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setIsActive(!user.getIsActive());
        user = userRepository.save(user);

        return getUserProfileDetail(user);
    }

    @Override
    public Page<UserProfileResponse> getAllUsersByRole(String roleName, Pageable pageable) {
        Page<User> users=userRepository.findByRoleNameAndIsActiveTrue(roleName, pageable);
        return users.map(this::getUserProfileDetail);
    }
}