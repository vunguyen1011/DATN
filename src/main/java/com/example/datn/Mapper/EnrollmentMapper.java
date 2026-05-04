package com.example.datn.Mapper;

import com.example.datn.DTO.Response.ClassSectionResponse;
import com.example.datn.DTO.Response.EnrollmentResponse;
import com.example.datn.DTO.Response.ScheduleResponse;
import com.example.datn.Model.ClassSection;
import com.example.datn.Model.Enrollment;
import com.example.datn.Model.Schedule;
import com.example.datn.Repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EnrollmentMapper {

    @Autowired
    private ScheduleRepository scheduleRepository;

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

        List<Schedule> schedules = scheduleRepository.findByClassSection_Id(classSection.getId());
        List<ScheduleResponse> scheduleResponses = schedules.stream().map(this::mapSchedule).collect(Collectors.toList());

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
                .schedules(scheduleResponses)
                .build();
    }

    private ScheduleResponse mapSchedule(Schedule schedule) {
        if (schedule == null) return null;

        String dayOfWeekName = null;
        if (schedule.getDayOfWeek() != null) {
            dayOfWeekName = schedule.getDayOfWeek() == 8 ? "Chủ nhật" : "Thứ " + schedule.getDayOfWeek();
        }

        return ScheduleResponse.builder()
                .id(schedule.getId())
                .classSectionId(schedule.getClassSection() != null ? schedule.getClassSection().getId() : null)
                .sectionCode(schedule.getClassSection() != null ? schedule.getClassSection().getSectionCode() : null)
                .subjectName(schedule.getClassSection() != null && schedule.getClassSection().getSubject() != null ? schedule.getClassSection().getSubject().getName() : null)
                .subjectCode(schedule.getClassSection() != null && schedule.getClassSection().getSubject() != null ? schedule.getClassSection().getSubject().getCode() : null)
                .roomId(schedule.getRoom() != null ? schedule.getRoom().getId() : null)
                .roomName(schedule.getRoom() != null ? schedule.getRoom().getName() : null)
                .lecturerId(schedule.getLecturer() != null ? schedule.getLecturer().getId() : null)
                .lecturerName(schedule.getLecturer() != null ? schedule.getLecturer().getFullName() : null)
                .lecturerCode(schedule.getLecturer() != null ? schedule.getLecturer().getLecturerCode() : null)
                .dayOfWeek(schedule.getDayOfWeek())
                .dayOfWeekName(dayOfWeekName)
                .startPeriod(schedule.getStartPeriod())
                .endPeriod(schedule.getEndPeriod())
                .build();
    }
}