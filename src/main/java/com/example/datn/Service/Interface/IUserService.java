package com.example.datn.Service.Interface;

import com.example.datn.DTO.Response.UserProfileResponse;
import com.example.datn.DTO.Response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IUserService {
    UserProfileResponse getMyInfo(String username);
    Page<UserResponse> getAllUsers(String searchKeyword, Pageable pageable);
    UserResponse getUserById(UUID id);
    UserResponse toggleUserStatus(UUID id);
}
