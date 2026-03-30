package com.example.datn.Service.Interface;

import com.example.datn.DTO.Response.UserProfileResponse;

public interface IUserService {
    UserProfileResponse getMyInfo(String username);
}
