package com.example.datn.Mapper;

import com.example.datn.DTO.Request.SubjectRequest;
import com.example.datn.DTO.Response.SubjectResponse;
import com.example.datn.Model.Subject;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {

    // 1. Chuyển từ Request (Client gửi lên) thành Entity (Để lưu DB)
    public Subject toEntity(SubjectRequest request) {
        return Subject.builder()
                .code(request.getCode())
                .name(request.getName())
                .credits(request.getCredits())
                .departmentName(request.getDepartmentName())
                .theoryPeriod(request.getTheoryPeriod())
                .practicePeriod(request.getPracticePeriod())
                .isActive(true) // Mặc định khi tạo mới
                .coffee(request.getCoffe())
                .build();
    }

    // 2. Chuyển từ Entity (Từ DB lấy lên) thành Response (Trả về cho Client)
    public SubjectResponse toResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .credits(subject.getCredits())
                .departmentName(subject.getDepartmentName())
                .theoryPeriod(subject.getTheoryPeriod())
                .practicePeriod(subject.getPracticePeriod())
                .isActive(subject.getIsActive())
                .coffee(subject.getCoffee())
                .build();
    }

    public void updateEntityFromRequest(Subject subject, SubjectRequest request) {
        subject.setCode(request.getCode());
        subject.setName(request.getName());
        subject.setCredits(request.getCredits());
        subject.setDepartmentName(request.getDepartmentName());
        subject.setTheoryPeriod(request.getTheoryPeriod());
        subject.setCoffee(request.getCoffe());
        subject.setPracticePeriod(request.getPracticePeriod());
    }
}