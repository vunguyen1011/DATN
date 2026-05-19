package com.example.datn.Service.Impl;

import com.example.datn.Model.Room;
import com.example.datn.Repository.RoomRepository;
import com.example.datn.Service.Interface.IRoomService;
import lombok.RequiredArgsConstructor;
import com.example.datn.Model.Semester;
import com.example.datn.Repository.SemesterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService implements IRoomService {
    private final RoomRepository roomRepository;
    private final SemesterRepository semesterRepository;
    
    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public List<Room> getAvailableRooms(Integer dayOfWeek, Integer startPeriod, Integer endPeriod) {
        if (dayOfWeek == null || startPeriod == null || endPeriod == null) {
            throw new IllegalArgumentException("dayOfWeek, startPeriod, and endPeriod must not be null");
        }
        if (startPeriod > endPeriod) {
            throw new IllegalArgumentException("startPeriod must be less than or equal to endPeriod");
        }
        
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ hiện tại"));
                
        return roomRepository.findAvailableRooms(currentSemester.getId(), dayOfWeek, startPeriod, endPeriod);
    }
}
