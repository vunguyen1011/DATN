package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.RegistrationPeriodRequest;
import com.example.datn.DTO.Response.RegistrationPeriodResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IRegistrationPeriodService {

    RegistrationPeriodResponse createRegistrationPeriod(RegistrationPeriodRequest request);

    RegistrationPeriodResponse updateRegistrationPeriod(UUID id, RegistrationPeriodRequest request);

    void deleteRegistrationPeriod(UUID id);

    RegistrationPeriodResponse getRegistrationPeriodById(UUID id);

    Page<RegistrationPeriodResponse> getAllRegistrationPeriods(Pageable pageable);

    Page<RegistrationPeriodResponse> getRegistrationPeriodsBySemester(UUID semesterId, Pageable pageable);
}
