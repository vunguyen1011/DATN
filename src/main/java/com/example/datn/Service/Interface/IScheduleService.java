package com.example.datn.Service.Interface;

import com.example.datn.DTO.Request.BulkScheduleLecturerRequest;
import com.example.datn.DTO.Request.ScheduleLecturerRequest;
import com.example.datn.DTO.Request.ScheduleRoomRequest;
import com.example.datn.DTO.Request.ScheduleTimeRequest;
import com.example.datn.DTO.Response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IScheduleService {

    ScheduleInitResponse createSchedule();

    AutoAssignResultResponse autoAssignRoomAndTime(UUID semesterId);

    ScheduleResponse assignTime(UUID scheduleId, ScheduleTimeRequest request);

    ScheduleResponse clearTime(UUID scheduleId);

    ScheduleResponse assignRoom(UUID scheduleId, ScheduleRoomRequest request);

    ScheduleResponse clearRoom(UUID scheduleId);

    void deleteSchedule(UUID id);

    Page<ScheduleResponse> getSchedulesBySemester(UUID semesterId, Pageable pageable);

    void exportSemesterScheduleToPdf(UUID semesterId, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException;

    ScheduleResponse assignLecturer(UUID scheduleId, ScheduleLecturerRequest request);

    ScheduleResponse clearLecturer(UUID scheduleId);

    List<ScheduleResponse> bulkAssignLecturer(BulkScheduleLecturerRequest request);

    List<SubjectResponse> getPendingSchedulesForHOD(String username, UUID semesterId);

    Page<ScheduleResponse> getSchedulesByLecturer(String lecturerCode, UUID semesterId, Pageable pageable);

    java.util.Map<String, List<ScheduleResponse>> getSchedulesByClassSection(UUID classSectionId);

    ScheduleResponse getScheduleById(UUID id);

    List<LecturerSuggestionResponse> suggestLecturersForSchedule(UUID scheduleId);

    List<SlotSuggestionResponse> suggestSlotsForSchedule(UUID scheduleId, int topN);
}