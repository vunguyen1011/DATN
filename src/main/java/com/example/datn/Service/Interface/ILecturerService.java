package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.LecturerUpdateRequest;
import com.example.datn.DTO.Response.UserProfileResponse.LecturerProfile;

import java.util.UUID;

public interface ILecturerService {
    LecturerProfile updateLecturerProfile(UUID lecturerId, LecturerUpdateRequest request);
}
