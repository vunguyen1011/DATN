package com.example.datn.Service.Impl;

import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.DTO.Response.UserResponse;
import com.example.datn.Exception.AppException;
import com.example.datn.Exception.ErrorCode;
import com.example.datn.Mapper.UserMapper;
import com.example.datn.Model.*;
import com.example.datn.Repository.LecturerRepository;
import com.example.datn.Repository.StudentRepository;
import com.example.datn.Repository.UserRepository;
import com.example.datn.Repository.UserRoleRepository;
import com.example.datn.Service.Interface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserProfileResponse getMyInfo(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<UserRole> roles = userRoleRepository.findByUser(user);
        Set<Role> roleSet = roles.stream().map(UserRole::getRole).collect(java.util.stream.Collectors.toSet());
        Lecturer lecturer = lecturerRepository.findByUser(user).orElse(null);
        Student student = studentRepository.findByUser(user).orElse(null);

        return UserMapper.toUserProfileResponse(user, student, lecturer, roleSet);
    }

    @Override
    public Page<UserResponse> getAllUsers(String searchKeyword, Pageable pageable) {
        Page<User> users;
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            users = userRepository.searchUsers(searchKeyword.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(UserMapper::fromUser);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserMapper.fromUser(user);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setIsActive(!user.getIsActive());
        user = userRepository.save(user);
        return UserMapper.fromUser(user);
    }

}
