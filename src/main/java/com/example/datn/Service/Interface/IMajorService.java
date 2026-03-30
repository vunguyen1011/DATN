package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.MajorRequest;
import com.example.datn.Model.Major;

import java.util.List;
import java.util.UUID;

public interface IMajorService {
        Major createMajor(MajorRequest request);
        List<Major> getAllMajors();

        Major getMajorById(UUID id);

        Major updateMajor(UUID id, MajorRequest request);

        void deleteMajor(UUID id);
        List<Major> searchMajors(String keyword);

    }
