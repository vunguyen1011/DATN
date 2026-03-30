package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.AdminClassRequest;
import com.example.datn.DTO.Response.AdminClassResponse;

import java.util.List;
import java.util.UUID;

public interface IAdminClassService {
    AdminClassResponse createAdminClass(AdminClassRequest request);
    List<AdminClassResponse> getAllAdminClasses();
    AdminClassResponse getAdminClassById(UUID id);
    AdminClassResponse updateAdminClass(UUID id, AdminClassRequest request);
    void deleteAdminClass(UUID id);
    List<AdminClassResponse> searchAdminClasses(String keyword);
}