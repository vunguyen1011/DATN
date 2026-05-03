package com.example.datn.Mapper;

import com.example.datn.DTO.Response.ClassSectionResponse;
import com.example.datn.DTO.Response.EnrollmentResponse;
import com.example.datn.Model.ClassSection;
import com.example.datn.Model.Enrollment;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {

    public EnrollmentResponse toResponse(Enrollment enrollment) {
        if (enrollment == null) return null;

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .status(enrollment.getStatus() != null ? enrollment.getStatus() : null)
                .enrollmentDate(enrollment.getEnrollmentDate())
                .classSection(mapClassSection(enrollment.getClassSection()))
                .build();
    }

    private ClassSectionResponse mapClassSection(ClassSection classSection) {
        if (classSection == null) return null;

        return ClassSectionResponse.builder()
                .id(classSection.getId())
                .sectionCode(classSection.getSectionCode())
                .capacity(classSection.getCapacity())
                .minStudents(classSection.getMinStudents())
                .enrolledCount(classSection.getEnrolledCount())
                .status(classSection.getStatus() != null ? classSection.getStatus() : null)
                .semesterId(classSection.getSemester() != null ? classSection.getSemester().getId() : null)
                .subjectName(classSection.getSubject() != null ? classSection.getSubject().getName() : null)
                .subjectCode(classSection.getSubject() != null ? classSection.getSubject().getCode() : null)
                .parentSectionId(classSection.getParentSection() != null ? classSection.getParentSection().getId() : null)
                .build();
    }
}